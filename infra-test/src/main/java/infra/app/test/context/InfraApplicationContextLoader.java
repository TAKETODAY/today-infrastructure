/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.test.context;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.app.Application;
import infra.app.ApplicationContextFactory;
import infra.app.ApplicationType;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.app.test.mock.web.InfraMockContext;
import infra.beans.BeanUtils;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.Order;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.Version;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.support.AbstractContextLoader;
import infra.test.context.support.AnnotationConfigContextLoaderUtils;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.context.web.WebMergedContextConfiguration;
import infra.test.util.TestPropertyValues;
import infra.test.util.TestPropertyValues.Type;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.mock.support.GenericWebApplicationContext;
import infra.web.server.reactive.context.GenericReactiveWebApplicationContext;

/**
 * A {@link ContextLoader} that can be used to test Infra applications (those that
 * normally startup using {@link Application}). Although this loader can be used
 * directly, most test will instead want to use it with
 * {@link InfraTest @InfraTest}.
 * <p>
 * The loader supports both standard {@link MergedContextConfiguration} as well as
 * {@link WebMergedContextConfiguration}. If {@link WebMergedContextConfiguration} is used
 * the context will either use a mock environment, or start the full embedded web
 * server.
 * <p>
 * If {@code @ActiveProfiles} are provided in the test class they will be used to create
 * the application context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraTest
 * @since 4.0
 */
public class InfraApplicationContextLoader extends AbstractContextLoader {

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration config) throws Exception {
    Class<?>[] configClasses = config.getClasses();
    String[] configLocations = config.getLocations();
    Assert.state(ObjectUtils.isNotEmpty(configClasses) || ObjectUtils.isNotEmpty(configLocations),
            () -> "No configuration classes or locations found in @InfraConfiguration. "
                    + "For default configuration detection to work you need today-framework 5.0 or better (found "
                    + Version.instance + ").");
    Application application = getApplication();
    application.setMainApplicationClass(config.getTestClass());
    application.addPrimarySources(Arrays.asList(configClasses));
    application.getSources().addAll(Arrays.asList(configLocations));
    List<ApplicationContextInitializer> initializers = getInitializers(config, application);
    if (config instanceof WebMergedContextConfiguration) {
      application.setApplicationType(ApplicationType.NETTY_WEB);
      if (!isEmbeddedWebEnvironment(config)) {
        new WebConfigurer().configure(config, application, initializers);
      }
    }
    else if (config instanceof ReactiveWebMergedContextConfiguration) {
      application.setApplicationType(ApplicationType.REACTIVE_WEB);
      if (!isEmbeddedWebEnvironment(config)) {
        application.setApplicationContextFactory(
                ApplicationContextFactory.from(GenericReactiveWebApplicationContext::new));
      }
    }
    else {
      application.setApplicationType(ApplicationType.NORMAL);
    }
    application.setInitializers(initializers);
    ConfigurableEnvironment environment = getEnvironment();
    if (environment != null) {
      prepareEnvironment(config, application, environment, false);
      application.setEnvironment(environment);
    }
    else {
      application.addListeners(new PrepareEnvironmentListener(config));
    }
    String[] args = InfraTestArgs.get(config.getContextCustomizers());
    return application.run(args);
  }

  private void prepareEnvironment(MergedContextConfiguration config, Application application,
          ConfigurableEnvironment environment, boolean applicationEnvironment) {
    setActiveProfiles(environment, config.getActiveProfiles(), applicationEnvironment);
    ResourceLoader resourceLoader = (application.getResourceLoader() != null) ? application.getResourceLoader()
            : new DefaultResourceLoader(null);
    TestPropertySourceUtils.addPropertiesFilesToEnvironment(environment, resourceLoader,
            config.getPropertySourceLocations());
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, getInlinedProperties(config));
  }

  private void setActiveProfiles(ConfigurableEnvironment environment, String[] profiles,
          boolean applicationEnvironment) {
    if (ObjectUtils.isEmpty(profiles)) {
      return;
    }
    if (!applicationEnvironment) {
      environment.setActiveProfiles(profiles);
    }
    String[] pairs = new String[profiles.length];
    for (int i = 0; i < profiles.length; i++) {
      pairs[i] = "infra.profiles.active[" + i + "]=" + profiles[i];
    }
    TestPropertyValues.of(pairs).applyTo(environment, Type.MAP, "active-test-profiles");
  }

  /**
   * Builds new {@link Application} instance. You can
   * override this method to add custom behavior
   *
   * @return {@link Application} instance
   */
  protected Application getApplication() {
    return new Application();
  }

  /**
   * Returns the {@link ConfigurableEnvironment} instance that should be applied to
   * {@link Application} or {@code null} to use the default. You can override this
   * method if you need a custom environment.
   *
   * @return a {@link ConfigurableEnvironment} instance
   */
  @Nullable
  protected ConfigurableEnvironment getEnvironment() {
    return null;
  }

  protected String[] getInlinedProperties(MergedContextConfiguration config) {
    ArrayList<String> properties = new ArrayList<>();
    // JMX bean names will clash if the same bean is used in multiple contexts
    properties.add("infra.jmx.enabled=false");
    properties.addAll(Arrays.asList(config.getPropertySourceProperties()));
    return StringUtils.toStringArray(properties);
  }

  /**
   * Return the {@link ApplicationContextInitializer initializers} that will be applied
   * to the context. By default this method will adapt {@link ContextCustomizer context
   * customizers}, add {@link Application#getInitializers() application
   * initializers} and add
   * {@link MergedContextConfiguration#getContextInitializerClasses() initializers
   * specified on the test}.
   *
   * @param config the source context configuration
   * @param application the application instance
   * @return the initializers to apply
   */
  protected List<ApplicationContextInitializer> getInitializers(MergedContextConfiguration config,
          Application application) {
    List<ApplicationContextInitializer> initializers = new ArrayList<>();
    for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
      initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
    }
    initializers.addAll(application.getInitializers());
    for (Class<? extends ApplicationContextInitializer> initializerClass : config
            .getContextInitializerClasses()) {
      initializers.add(BeanUtils.newInstance(initializerClass));
    }
    if (config.getParent() != null) {
      initializers.add(new ParentContextApplicationContextInitializer(config.getParentApplicationContext()));
    }
    return initializers;
  }

  private boolean isEmbeddedWebEnvironment(MergedContextConfiguration config) {
    return MergedAnnotations.from(config.getTestClass(), SearchStrategy.TYPE_HIERARCHY)
            .get(InfraTest.class)
            .getValue("webEnvironment", WebEnvironment.class, WebEnvironment.NONE)
            .isEmbedded();
  }

  @Override
  public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
    super.processContextConfiguration(configAttributes);
    if (!configAttributes.hasResources()) {
      Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(configAttributes.getDeclaringClass());
      configAttributes.setClasses(defaultConfigClasses);
    }
  }

  /**
   * Detect the default configuration classes for the supplied test class. By default
   * simply delegates to
   * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses}.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @return an array of default configuration classes, potentially empty but never
   * {@code null}
   * @see AnnotationConfigContextLoaderUtils
   */
  protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
    return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
  }

  @Override
  public ApplicationContext loadContext(String... locations) throws Exception {
    throw new UnsupportedOperationException(
            "ApplicationContextLoader does not support the loadContext(String...) method");
  }

  @Override
  protected String[] getResourceSuffixes() {
    return new String[] { "-context.xml", "Context.groovy" };
  }

  @Override
  protected String getResourceSuffix() {
    throw new IllegalStateException();
  }

  /**
   * Inner class to configure {@link WebMergedContextConfiguration}.
   */
  private static final class WebConfigurer {

    void configure(MergedContextConfiguration configuration, Application application, List<ApplicationContextInitializer> initializers) {
      WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
      addMockContext(initializers, webConfiguration);
      application.setApplicationContextFactory(webApplicationType -> new GenericWebApplicationContext());
    }

    private void addMockContext(List<ApplicationContextInitializer> initializers,
            WebMergedContextConfiguration webConfiguration) {
      InfraMockContext mockContext = new InfraMockContext(
              webConfiguration.getResourceBasePath());
      initializers.add(0, new MockContextApplicationContextInitializer(mockContext, true));
    }
  }

  /**
   * Adapts a {@link ContextCustomizer} to a {@link ApplicationContextInitializer} so
   * that it can be triggered via {@link Application}.
   */
  private static class ContextCustomizerAdapter implements ApplicationContextInitializer {

    private final ContextCustomizer contextCustomizer;

    private final MergedContextConfiguration config;

    ContextCustomizerAdapter(ContextCustomizer contextCustomizer, MergedContextConfiguration config) {
      this.contextCustomizer = contextCustomizer;
      this.config = config;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      this.contextCustomizer.customizeContext(applicationContext, this.config);
    }

  }

  /**
   * {@link ApplicationContextInitializer} used to set the parent context.
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  private static class ParentContextApplicationContextInitializer implements ApplicationContextInitializer {

    private final ApplicationContext parent;

    ParentContextApplicationContextInitializer(ApplicationContext parent) {
      this.parent = parent;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      applicationContext.setParent(this.parent);
    }

  }

  /**
   * {@link ApplicationListener} used to prepare the application created environment.
   */
  private class PrepareEnvironmentListener
          implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, PriorityOrdered {

    private final MergedContextConfiguration config;

    PrepareEnvironmentListener(MergedContextConfiguration config) {
      this.config = config;
    }

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
      prepareEnvironment(this.config, event.getApplication(), event.getEnvironment(), true);
    }

  }

}
