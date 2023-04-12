/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.InfraConfiguration;
import cn.taketoday.framework.test.context.InfraTest.WebEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.support.TestPropertySourceUtils;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.context.web.WebMergedContextConfiguration;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

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
 * @see InfraTest
 * @see TestConfiguration
 * @since 4.0
 */
public class InfraTestContextBootstrapper extends DefaultTestContextBootstrapper {

  private static final String[] WEB_ENVIRONMENT_CLASSES = { "jakarta.servlet.Servlet",
          "cn.taketoday.web.servlet.ConfigurableWebApplicationContext" };

  private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "cn.taketoday."
          + "web.reactive.DispatcherHandler";

  private static final String MVC_WEB_ENVIRONMENT_CLASS = "cn.taketoday.web.servlet.DispatcherServlet";

  private static final String ACTIVATE_SERVLET_LISTENER = "cn.taketoday.test."
          + "context.web.ServletTestExecutionListener.activateListener";

  private static final Logger logger = LoggerFactory.getLogger(InfraTestContextBootstrapper.class);

  @Override
  public TestContext buildTestContext() {
    TestContext context = super.buildTestContext();
    verifyConfiguration(context.getTestClass());
    WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
    if (webEnvironment == WebEnvironment.MOCK && deduceWebApplicationType() == ApplicationType.SERVLET_WEB) {
      context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
    }
    else if (webEnvironment != null && webEnvironment.isEmbedded()) {
      context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
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
  protected ContextLoader resolveContextLoader(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributesList) {
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
      if (webApplicationType == ApplicationType.SERVLET_WEB
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
            .orElseGet(this::deduceWebApplicationType);
  }

  private ApplicationType deduceWebApplicationType() {
    if (ClassUtils.isPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null)
            && !ClassUtils.isPresent(MVC_WEB_ENVIRONMENT_CLASS, null)) {
      return ApplicationType.REACTIVE_WEB;
    }
    for (String className : WEB_ENVIRONMENT_CLASSES) {
      if (!ClassUtils.isPresent(className, null)) {
        return ApplicationType.NONE_WEB;
      }
    }
    return ApplicationType.SERVLET_WEB;
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
            .get(WebAppConfiguration.class).getValue(MergedAnnotation.VALUE, String.class)
            .orElse("src/main/webapp");
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

  private boolean isFromConfiguration(MergedContextConfiguration candidateConfig,
          ContextConfiguration configuration) {
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
  protected WebEnvironment getWebEnvironment(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.webEnvironment() : null;
  }

  protected Class<?>[] getClasses(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.classes() : null;
  }

  protected String[] getProperties(Class<?> testClass) {
    InfraTest annotation = getAnnotation(testClass);
    return (annotation != null) ? annotation.properties() : null;
  }

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
            mergedConfig.getPropertySourceLocations(), propertySourceProperties, contextCustomizers,
            mergedConfig.getContextLoader(), getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
  }

}
