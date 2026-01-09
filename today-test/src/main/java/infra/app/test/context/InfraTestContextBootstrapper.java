/*
 * Copyright 2012-present the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.app.ApplicationType;
import infra.app.InfraConfiguration;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MapConfigurationPropertySource;
import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextHierarchy;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContext;
import infra.test.context.TestContextAnnotationUtils;
import infra.test.context.TestContextBootstrapper;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.DefaultTestContextBootstrapper;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.context.web.WebAppConfiguration;
import infra.test.context.web.WebMergedContextConfiguration;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * {@link TestContextBootstrapper} for Infra. Provides support for
 * {@link InfraTest @InfraTest} and may also be used directly or subclassed.
 * Provides the following features over and above {@link DefaultTestContextBootstrapper}:
 * <ul>
 * <li>Uses {@link InfraApplicationContextLoader} as the
 * {@link #getDefaultContextLoaderClass(Class) default context loader}.</li>
 * <li>Automatically searches for a
 * {@link InfraConfiguration @InfraConfiguration} when required.</li>
 * <li>Allows custom {@link Environment} {@link #getProperties(Class)} to be defined.</li>
 * <li>Provides support for different {@link WebEnvironment webEnvironment} modes.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Lorenzo Dee
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraTest
 * @see TestConfiguration
 * @since 4.0
 */
public class InfraTestContextBootstrapper extends DefaultTestContextBootstrapper {

  private static final String ACTIVATE_LISTENER
          = "infra.test.context.web.MockTestExecutionListener.activateListener";

  private static final Logger logger = LoggerFactory.getLogger(InfraTestContextBootstrapper.class);

  @Override
  public TestContext buildTestContext() {
    TestContext context = super.buildTestContext();
    verifyConfiguration(context.getTestClass());
    WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
    if (webEnvironment == WebEnvironment.MOCK && ApplicationType.forDefaults() == ApplicationType.NETTY_WEB) {
      context.setAttribute(ACTIVATE_LISTENER, true);
    }
    else if (webEnvironment != null && webEnvironment.isEmbedded()) {
      context.setAttribute(ACTIVATE_LISTENER, false);
    }
    return context;
  }

  @Override
  protected List<TestExecutionListener> getDefaultTestExecutionListeners() {
    List<TestExecutionListener> listeners = super.getDefaultTestExecutionListeners();
    List<TestExecutionListenersPostProcessor> postProcessors = TodayStrategies.find(
            TestExecutionListenersPostProcessor.class, getClass().getClassLoader());
    for (TestExecutionListenersPostProcessor postProcessor : postProcessors) {
      listeners = postProcessor.postProcessListeners(listeners);
    }
    return listeners;
  }

  @Override
  protected ContextLoader resolveContextLoader(Class<?> testClass, List<ContextConfigurationAttributes> configAttributesList) {
    Class<?>[] classes = getClasses(testClass);
    if (ObjectUtils.isNotEmpty(classes)) {
      for (ContextConfigurationAttributes configAttributes : configAttributesList) {
        addConfigAttributesClasses(configAttributes, classes);
      }
    }
    return super.resolveContextLoader(testClass, configAttributesList);
  }

  private void addConfigAttributesClasses(ContextConfigurationAttributes configAttributes, Class<?>[] classes) {
    Set<Class<?>> combined = new LinkedHashSet<>(Arrays.asList(classes));
    if (configAttributes.getClasses() != null) {
      combined.addAll(Arrays.asList(configAttributes.getClasses()));
    }
    configAttributes.setClasses(ClassUtils.toClassArray(combined));
  }

  @Override
  protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
    return InfraApplicationContextLoader.class;
  }

  @Override
  protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
    List<String> propertySourceProperties = getAndProcessPropertySourceProperties(mergedConfig);
    mergedConfig = createModifiedConfig(mergedConfig, classes, StringUtils.toStringArray(propertySourceProperties));
    WebEnvironment webEnvironment = getWebEnvironment(mergedConfig.getTestClass());
    if (webEnvironment != null && isWebEnvironmentSupported(mergedConfig)) {
      ApplicationType webApplicationType = getApplicationType(mergedConfig);
      if (webApplicationType == ApplicationType.NETTY_WEB
              && (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
        mergedConfig = new WebMergedContextConfiguration(mergedConfig, determineResourceBasePath(mergedConfig));
      }
      else if (webApplicationType == ApplicationType.REACTIVE_WEB
              && (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
        return new ReactiveWebMergedContextConfiguration(mergedConfig);
      }
    }
    return mergedConfig;
  }

  private ApplicationType getApplicationType(MergedContextConfiguration configuration) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(
            TestPropertySourceUtils.convertInlinedPropertiesToMap(configuration.getPropertySourceProperties()));
    Binder binder = new Binder(source);
    return binder.bind("app.main.application-type", Bindable.of(ApplicationType.class))
            .orElseGet(ApplicationType::forDefaults);
  }

  /**
   * Determines the resource base path for web applications using the value of
   * {@link WebAppConfiguration @WebAppConfiguration}, if any, on the test class of the
   * given {@code configuration}. Defaults to {@code src/main/webapp} in its absence.
   *
   * @param configuration the configuration to examine
   * @return the resource base path
   */
  protected String determineResourceBasePath(MergedContextConfiguration configuration) {
    return MergedAnnotations.from(configuration.getTestClass(), SearchStrategy.TYPE_HIERARCHY)
            .get(WebAppConfiguration.class)
            .getValue(MergedAnnotation.VALUE, String.class, "src/main/webapp");
  }

  private boolean isWebEnvironmentSupported(MergedContextConfiguration mergedConfig) {
    Class<?> testClass = mergedConfig.getTestClass();
    ContextHierarchy hierarchy = AnnotationUtils.getAnnotation(testClass, ContextHierarchy.class);
    if (hierarchy == null || hierarchy.value().length == 0) {
      return true;
    }
    ContextConfiguration[] configurations = hierarchy.value();
    return isFromConfiguration(mergedConfig, configurations[configurations.length - 1]);
  }

  private boolean isFromConfiguration(MergedContextConfiguration candidateConfig, ContextConfiguration configuration) {
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(candidateConfig.getTestClass(),
            configuration);
    Set<Class<?>> configurationClasses = new HashSet<>(Arrays.asList(attributes.getClasses()));
    for (Class<?> candidate : candidateConfig.getClasses()) {
      if (configurationClasses.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
    Class<?>[] classes = mergedConfig.getClasses();
    if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
      return classes;
    }
    Class<?> found = new AnnotatedClassFinder(
            InfraConfiguration.class).findFromClass(mergedConfig.getTestClass());
    Assert.state(found != null, "Unable to find a @InfraConfiguration, you need to use "
            + "@ContextConfiguration or @InfraTest(classes=...) with your test");
    logger.info("Found @InfraConfiguration {} for test {}", found.getName(), mergedConfig.getTestClass());
    return merge(found, classes);
  }

  private boolean containsNonTestComponent(Class<?>[] classes) {
    for (Class<?> candidate : classes) {
      if (!MergedAnnotations.from(candidate, SearchStrategy.INHERITED_ANNOTATIONS)
              .isPresent(TestConfiguration.class)) {
        return true;
      }
    }
    return false;
  }

  private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
    Class<?>[] result = new Class<?>[existing.length + 1];
    result[0] = head;
    System.arraycopy(existing, 0, result, 1, existing.length);
    return result;
  }

  private List<String> getAndProcessPropertySourceProperties(MergedContextConfiguration mergedConfig) {
    List<String> propertySourceProperties = new ArrayList<>(
            Arrays.asList(mergedConfig.getPropertySourceProperties()));
    String differentiator = getDifferentiatorPropertySourceProperty();
    if (differentiator != null) {
      propertySourceProperties.add(differentiator);
    }
    processPropertySourceProperties(mergedConfig, propertySourceProperties);
    return propertySourceProperties;
  }

  /**
   * Return a "differentiator" property to ensure that there is something to
   * differentiate regular tests and bootstrapped tests. Without this property a cached
   * context could be returned that wasn't created by this bootstrapper. By default uses
   * the bootstrapper class as a property.
   *
   * @return the differentiator or {@code null}
   */
  @Nullable
  protected String getDifferentiatorPropertySourceProperty() {
    return getClass().getName() + "=true";
  }

  /**
   * Post process the property source properties, adding or removing elements as
   * required.
   *
   * @param mergedConfig the merged context configuration
   * @param propertySourceProperties the property source properties to process
   */
  protected void processPropertySourceProperties(
          MergedContextConfiguration mergedConfig, List<String> propertySourceProperties) {
    Class<?> testClass = mergedConfig.getTestClass();
    String[] properties = getProperties(testClass);
    if (ObjectUtils.isNotEmpty(properties)) {
      // Added first so that inlined properties from @TestPropertySource take
      // precedence
      propertySourceProperties.addAll(0, Arrays.asList(properties));
    }
    if (getWebEnvironment(testClass) == WebEnvironment.RANDOM_PORT) {
      propertySourceProperties.add("server.port=0");
    }
  }

  /**
   * Return the {@link WebEnvironment} type for this test or null if undefined.
   *
   * @param testClass the source test class
   * @return the {@link WebEnvironment} or {@code null}
   */
  @Nullable
  protected WebEnvironment getWebEnvironment(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.webEnvironment() : null;
  }

  @Nullable
  protected Class<?>[] getClasses(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.classes() : null;
  }

  protected String @Nullable [] getProperties(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.properties() : null;
  }

  @Nullable
  protected InfraTest getAnnotation(Class<?> testClass) {
    return TestContextAnnotationUtils.findMergedAnnotation(testClass, InfraTest.class);
  }

  protected void verifyConfiguration(Class<?> testClass) {
    InfraTest infraTest = getAnnotation(testClass);
    if (infraTest != null && isListeningOnPort(infraTest.webEnvironment()) && MergedAnnotations
            .from(testClass, SearchStrategy.INHERITED_ANNOTATIONS).isPresent(WebAppConfiguration.class)) {
      throw new IllegalStateException("@WebAppConfiguration should only be used "
              + "with @InfraTest when @InfraTest is configured with a "
              + "mock web environment. Please remove @WebAppConfiguration or reconfigure @InfraTest.");
    }
  }

  private boolean isListeningOnPort(WebEnvironment webEnvironment) {
    return webEnvironment == WebEnvironment.DEFINED_PORT || webEnvironment == WebEnvironment.RANDOM_PORT;
  }

  /**
   * Create a new {@link MergedContextConfiguration} with different classes.
   *
   * @param mergedConfig the source config
   * @param classes the replacement classes
   * @return a new {@link MergedContextConfiguration}
   */
  protected final MergedContextConfiguration createModifiedConfig(
          MergedContextConfiguration mergedConfig, Class<?>[] classes) {
    return createModifiedConfig(mergedConfig, classes, mergedConfig.getPropertySourceProperties());
  }

  /**
   * Create a new {@link MergedContextConfiguration} with different classes and
   * properties.
   *
   * @param mergedConfig the source config
   * @param classes the replacement classes
   * @param propertySourceProperties the replacement properties
   * @return a new {@link MergedContextConfiguration}
   */
  protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
          Class<?>[] classes, String[] propertySourceProperties) {
    Set<ContextCustomizer> contextCustomizers = new LinkedHashSet<>(mergedConfig.getContextCustomizers());
    contextCustomizers.add(new InfraTestArgs(mergedConfig.getTestClass()));
    contextCustomizers.add(new InfraTestWebEnvironment(mergedConfig.getTestClass()));
    return new MergedContextConfiguration(mergedConfig.getTestClass(), mergedConfig.getLocations(), classes,
            mergedConfig.getContextInitializerClasses(), mergedConfig.getActiveProfiles(),
            mergedConfig.getPropertySourceDescriptors(), propertySourceProperties, contextCustomizers,
            mergedConfig.getContextLoader(), getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
  }

}
