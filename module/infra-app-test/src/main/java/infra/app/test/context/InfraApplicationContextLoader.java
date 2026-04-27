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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.RuntimeHints;
import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.app.ApplicationContextFactory;
import infra.app.ApplicationHook;
import infra.app.ApplicationStartupListener;
import infra.app.ApplicationType;
import infra.app.Banner;
import infra.app.ConfigurableBootstrapContext;
import infra.app.InfraConfiguration;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.app.test.context.InfraTest.UseMainMethod;
import infra.app.test.mock.web.InfraMockContext;
import infra.beans.BeanUtils;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.aot.AotApplicationContextInitializer;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.Order;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextLoadException;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.aot.AotContextLoader;
import infra.test.context.support.AbstractContextLoader;
import infra.test.context.support.AnnotationConfigContextLoaderUtils;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.context.web.WebMergedContextConfiguration;
import infra.test.util.TestPropertyValues;
import infra.test.util.TestPropertyValues.Type;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;
import infra.util.function.ThrowingSupplier;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.support.GenericWebApplicationContext;
import infra.web.reactive.context.GenericReactiveWebApplicationContext;

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
public class InfraApplicationContextLoader extends AbstractContextLoader implements AotContextLoader {

  private static final Object NONE = new Object();

  private static final Consumer<Application> ALREADY_CONFIGURED = (application) -> {
  };

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration config) throws Exception {
    return loadContext(config, Mode.STANDARD, null, null);
  }

  @Override
  public ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints) throws Exception {
    return loadContext(mergedConfig, Mode.AOT_PROCESSING, null,
            mainMethod -> runtimeHints.reflection().registerMethod(mainMethod, ExecutableMode.INVOKE));
  }

  @Override
  public ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig, ApplicationContextInitializer initializer) throws Exception {
    return loadContext(mergedConfig, Mode.AOT_RUNTIME, initializer, null);
  }

  protected ApplicationContext loadContext(MergedContextConfiguration mergedConfig, Mode mode,
          @Nullable ApplicationContextInitializer initializer, @Nullable Consumer<Method> mainMethodConsumer) throws Exception {
    assertHasClassesOrLocations(mergedConfig);
    InfraTestAnnotation annotation = InfraTestAnnotation.get(mergedConfig);
    String[] args = annotation.getArgs();
    UseMainMethod useMainMethod = annotation.getUseMainMethod();
    Method mainMethod = getMainMethod(mergedConfig, useMainMethod);

    if (mainMethod != null) {
      if (mainMethodConsumer != null) {
        mainMethodConsumer.accept(mainMethod);
      }
      ContextLoaderHook hook = new ContextLoaderHook(mode, initializer, application -> configure(mergedConfig, application));
      return hook.runMain(() -> {
        if (mainMethod.getParameterCount() == 0) {
          ReflectionUtils.invokeMethod(mainMethod, null);
        }
        else {
          ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { args });
        }
      });
    }

    Application application = createApplication();
    configure(mergedConfig, application);
    ContextLoaderHook hook = new ContextLoaderHook(mode, initializer, ALREADY_CONFIGURED);
    return hook.run(() -> application.run(args));
  }

  protected void assertHasClassesOrLocations(MergedContextConfiguration mergedConfig) {
    Assert.state(mergedConfig.hasResources(),
            () -> "No configuration classes or locations found. Check your test's configuration.");
  }

  private @Nullable Method getMainMethod(MergedContextConfiguration mergedConfig, UseMainMethod useMainMethod) {
    if (useMainMethod == UseMainMethod.NEVER) {
      return null;
    }
    Assert.state(mergedConfig.getParent() == null,
            () -> "UseMainMethod.%s cannot be used with @ContextHierarchy tests".formatted(useMainMethod));
    Class<?> infraConfiguration = Arrays.stream(mergedConfig.getClasses())
            .filter(this::isInfraConfiguration)
            .findFirst()
            .orElse(null);
    Assert.state(infraConfiguration != null || useMainMethod == UseMainMethod.WHEN_AVAILABLE,
            "Cannot use main method as no @InfraConfiguration-annotated class is available");
    Method mainMethod = findMainMethod(infraConfiguration);
    Assert.state(mainMethod != null || useMainMethod == UseMainMethod.WHEN_AVAILABLE,
            () -> "Main method not found on '%s'".formatted((infraConfiguration != null) ? infraConfiguration.getName() : null));
    return mainMethod;
  }

  private static @Nullable Method findMainMethod(@Nullable Class<?> type) {
    return type != null ? findMainJavaMethod(type) : null;
  }

  private static @Nullable Method findMainJavaMethod(Class<?> type) {
    try {
      Method method = getMainMethod(type);
      if (Modifier.isStatic(method.getModifiers())) {
        method.setAccessible(true);
        return method;
      }
    }
    catch (Exception ex) {
      // Ignore
    }
    return null;
  }

  private static Method getMainMethod(Class<?> type) throws NoSuchMethodException {
    try {
      return type.getDeclaredMethod("main", String[].class);
    }
    catch (NoSuchMethodException ex) {
      return type.getDeclaredMethod("main");
    }

  }

  private boolean isInfraConfiguration(Class<?> candidate) {
    return MergedAnnotations.from(candidate, SearchStrategy.TYPE_HIERARCHY)
            .isPresent(InfraConfiguration.class);
  }

  protected void configure(MergedContextConfiguration config, Application application) {
    application.setMainApplicationClass(config.getTestClass());
    application.addPrimarySources(Arrays.asList(config.getClasses()));
    application.getSources().addAll(Arrays.asList(config.getLocations()));

    List<ApplicationContextInitializer> initializers = getInitializers(config, application);
    if (config instanceof WebMergedContextConfiguration) {
      application.setApplicationType(ApplicationType.WEB);
      if (!isEmbeddedWebEnvironment(config)) {
        new WebConfigurer().configure(config, initializers);
      }
    }
    else if (config instanceof ReactiveWebMergedContextConfiguration) {
      application.setApplicationType(ApplicationType.REACTIVE_WEB);
    }
    else {
      application.setApplicationType(ApplicationType.NORMAL);
    }

    application.setApplicationContextFactory(getApplicationContextFactory(config));
    if (config.getParent() != null) {
      application.setBannerMode(Banner.Mode.OFF);
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
  }

  /**
   * Return the {@link ApplicationContextFactory} that should be used for the test. By
   * defaults this method will return a factory that will create an appropriate
   * {@link ApplicationContext} for the {@link ApplicationType}.
   *
   * @param mergedConfig the merged context configuration
   * @return the application context factory to use
   * @since 5.0
   */
  protected ApplicationContextFactory getApplicationContextFactory(MergedContextConfiguration mergedConfig) {
    return applicationType -> {
      if (applicationType != ApplicationType.NORMAL && !isEmbeddedWebEnvironment(mergedConfig)) {
        if (applicationType == ApplicationType.REACTIVE_WEB) {
          return new GenericReactiveWebApplicationContext();
        }
        if (applicationType == ApplicationType.WEB) {
          return new GenericWebApplicationContext();
        }
      }
      return ApplicationContextFactory.DEFAULT.create(applicationType);
    };
  }

  private void prepareEnvironment(MergedContextConfiguration config, Application application,
          ConfigurableEnvironment environment, boolean applicationEnvironment) {
    setActiveProfiles(environment, config.getActiveProfiles(), applicationEnvironment);
    ResourceLoader resourceLoader = application.getResourceLoader() != null
            ? application.getResourceLoader() : new DefaultResourceLoader(null);
    TestPropertySourceUtils.addPropertiesFilesToEnvironment(environment, resourceLoader, config.getPropertySourceLocations());
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
  protected Application createApplication() {
    return new Application();
  }

  /**
   * Returns the {@link ConfigurableEnvironment} instance that should be applied to
   * {@link Application} or {@code null} to use the default. You can override this
   * method if you need a custom environment.
   *
   * @return a {@link ConfigurableEnvironment} instance
   */
  protected @Nullable ConfigurableEnvironment getEnvironment() {
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
   * to the context. By defaults this method will adapt {@link ContextCustomizer context
   * customizers}, add {@link Application#getInitializers() application
   * initializers} and add
   * {@link MergedContextConfiguration#getContextInitializerClasses() initializers
   * specified on the test}.
   *
   * @param config the source context configuration
   * @param application the application instance
   * @return the initializers to apply
   */
  protected List<ApplicationContextInitializer> getInitializers(MergedContextConfiguration config, Application application) {
    List<ApplicationContextInitializer> initializers = new ArrayList<>();
    for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
      initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
    }
    initializers.addAll(application.getInitializers());
    for (var initializerClass : config.getContextInitializerClasses()) {
      initializers.add(BeanUtils.newInstance(initializerClass));
    }
    if (config.getParent() != null) {
      initializers.add(new ParentContextApplicationContextInitializer(config.getParentApplicationContext()));
    }
    return initializers;
  }

  private boolean isEmbeddedWebEnvironment(MergedContextConfiguration config) {
    return InfraTestAnnotation.get(config).getWebEnvironment().isEmbedded();
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

    void configure(MergedContextConfiguration configuration, List<ApplicationContextInitializer> initializers) {
      WebMergedContextConfiguration webConfiguration = (WebMergedContextConfiguration) configuration;
      addMockContext(initializers, webConfiguration);
    }

    private void addMockContext(List<ApplicationContextInitializer> initializers, WebMergedContextConfiguration webConfiguration) {
      InfraMockContext mockContext = new InfraMockContext(webConfiguration.getResourceBasePath());
      initializers.add(0, new DefensiveWebApplicationContextInitializer(
              new MockContextApplicationContextInitializer(mockContext, true)));
    }

    /**
     * Decorator for {@link MockContextApplicationContextInitializer} that prevents
     * a failure when the context type is not as was predicted when the initializer
     * was registered.
     */
    private static final class DefensiveWebApplicationContextInitializer
            implements ApplicationContextInitializer {

      private final MockContextApplicationContextInitializer delegate;

      private DefensiveWebApplicationContextInitializer(MockContextApplicationContextInitializer delegate) {
        this.delegate = delegate;
      }

      @Override
      public void initialize(ConfigurableApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableWebApplicationContext webApplicationContext) {
          this.delegate.initialize(webApplicationContext);
        }
      }

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

    private final @Nullable ApplicationContext parent;

    ParentContextApplicationContextInitializer(@Nullable ApplicationContext parent) {
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

  /**
   * Modes that the {@link InfraApplicationContextLoader} can operate.
   */
  protected enum Mode {

    /**
     * Load for regular usage.
     *
     * @see SmartContextLoader#loadContext
     */
    STANDARD,

    /**
     * Load for AOT processing.
     *
     * @see AotContextLoader#loadContextForAotProcessing
     */
    AOT_PROCESSING,

    /**
     * Load for AOT runtime.
     *
     * @see AotContextLoader#loadContextForAotRuntime
     */
    AOT_RUNTIME
  }

  /**
   * {@link ApplicationHook} used to capture {@link ApplicationContext} instances
   * and to trigger early exit for the {@link Mode#AOT_PROCESSING} mode.
   */
  protected static class ContextLoaderHook implements ApplicationHook {

    private final Mode mode;

    private final @Nullable ApplicationContextInitializer initializer;

    private final Consumer<Application> configurer;

    private final List<ApplicationContext> contexts = Collections.synchronizedList(new ArrayList<>());

    private final List<ApplicationContext> failedContexts = Collections.synchronizedList(new ArrayList<>());

    ContextLoaderHook(Mode mode, @Nullable ApplicationContextInitializer initializer, Consumer<Application> configurer) {
      this.mode = mode;
      this.initializer = initializer;
      this.configurer = configurer;
    }

    @Override
    public @Nullable ApplicationStartupListener getStartupListener(Application application) {
      return new ApplicationStartupListener() {

        @Override
        public void starting(ConfigurableBootstrapContext bootstrapContext,
                @Nullable Class<?> mainApplicationClass, ApplicationArguments arguments) {
          ContextLoaderHook.this.configurer.accept(application);
          if (ContextLoaderHook.this.mode == Mode.AOT_RUNTIME) {
            Assert.state(ContextLoaderHook.this.initializer != null, "'initializer' is required");
            application.addInitializers(
                    (AotApplicationContextInitializer) ContextLoaderHook.this.initializer::initialize);
          }
        }

        @Override
        public void contextLoaded(ConfigurableApplicationContext context) {
          ContextLoaderHook.this.contexts.add(context);
          if (ContextLoaderHook.this.mode == Mode.AOT_PROCESSING) {
            throw new Application.AbandonedRunException(context);
          }
        }

        @Override
        public void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
          if (context != null) {
            ContextLoaderHook.this.failedContexts.add(context);
          }
        }

      };
    }

    private ApplicationContext runMain(Runnable action) throws Exception {
      return run(() -> {
        action.run();
        return NONE;
      });
    }

    private <T> ApplicationContext run(ThrowingSupplier<T> action) throws Exception {
      try {
        Object result = Application.withHook(this, action);
        if (result instanceof ApplicationContext context) {
          return context;
        }
      }
      catch (Application.AbandonedRunException ex) {
        // Ignore
      }
      catch (Exception ex) {
        if (this.failedContexts.size() == 1) {
          throw new ContextLoadException(this.failedContexts.get(0), ex);
        }
        throw ex;
      }
      List<ApplicationContext> rootContexts = this.contexts.stream()
              .filter((context) -> context.getParent() == null)
              .toList();
      Assert.state(!rootContexts.isEmpty(), "No root application context located");
      Assert.state(rootContexts.size() == 1, "No unique root application context located");
      return rootContexts.get(0);
    }

  }

}
