/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.BootstrapContext;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.TestExecutionListeners.MergeMode;
import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.test.context.util.TestContextFactoriesUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract implementation of the {@link TestContextBootstrapper} interface which
 * provides most of the behavior required by a bootstrapper.
 *
 * <p>Concrete subclasses typically will only need to provide implementations for
 * the following methods:
 * <ul>
 * <li>{@link #getDefaultContextLoaderClass}
 * <li>{@link #processMergedContextConfiguration}
 * </ul>
 *
 * <p>To plug in custom
 * {@link ContextCache ContextCache}
 * support, override {@link #getCacheAwareContextLoaderDelegate()}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {
  private static final Logger log = LoggerFactory.getLogger(AbstractTestContextBootstrapper.class);

  @Nullable
  private BootstrapContext bootstrapContext;

  @Override
  public void setBootstrapContext(BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
  }

  @Override
  public BootstrapContext getBootstrapContext() {
    Assert.state(this.bootstrapContext != null, "No BootstrapContext set");
    return this.bootstrapContext;
  }

  /**
   * Build a new {@link DefaultTestContext} using the {@linkplain Class test class}
   * in the {@link BootstrapContext} associated with this bootstrapper and
   * by delegating to {@link #buildMergedContextConfiguration()} and
   * {@link #getCacheAwareContextLoaderDelegate()}.
   * <p>Concrete subclasses may choose to override this method to return a
   * custom {@link TestContext} implementation.
   */
  @Override
  public TestContext buildTestContext() {
    return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
            getCacheAwareContextLoaderDelegate());
  }

  @Override
  public final List<TestExecutionListener> getTestExecutionListeners() {
    Class<?> clazz = getBootstrapContext().getTestClass();
    Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
    ArrayList<TestExecutionListener> listeners = new ArrayList<>(8);
    boolean usingDefaults = false;

    AnnotationDescriptor<TestExecutionListeners> descriptor =
            TestContextAnnotationUtils.findAnnotationDescriptor(clazz, annotationType);

    // Use defaults?
    if (descriptor == null) {
      if (log.isTraceEnabled()) {
        log.trace("@TestExecutionListeners is not present for class [{}]: using defaults.", clazz.getName());
      }
      usingDefaults = true;
      listeners.addAll(getDefaultTestExecutionListeners());
    }
    else {
      // Traverse the class hierarchy...
      while (descriptor != null) {
        Class<?> declaringClass = descriptor.getDeclaringClass();
        TestExecutionListeners testExecutionListeners = descriptor.getAnnotation();
        if (log.isTraceEnabled()) {
          log.trace("Retrieved @TestExecutionListeners [{}] for declaring class [{}].",
                  testExecutionListeners, declaringClass.getName());
        }

        boolean inheritListeners = testExecutionListeners.inheritListeners();
        AnnotationDescriptor<TestExecutionListeners> parentDescriptor = descriptor.next();

        // If there are no listeners to inherit, we might need to merge the
        // locally declared listeners with the defaults.
        if ((!inheritListeners || parentDescriptor == null) &&
                testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS) {
          if (log.isTraceEnabled()) {
            log.trace("Merging default listeners with listeners configured via " +
                    "@TestExecutionListeners for class [{}].", descriptor.getRootDeclaringClass().getName());
          }
          usingDefaults = true;
          listeners.addAll(getDefaultTestExecutionListeners());
        }

        listeners.addAll(0, instantiateListeners(testExecutionListeners.listeners()));

        descriptor = (inheritListeners ? parentDescriptor : null);
      }
    }

    if (usingDefaults) {
      // Remove possible duplicates if we loaded default listeners.
      ArrayList<TestExecutionListener> uniqueListeners = new ArrayList<>(listeners.size());
      listeners.forEach(listener -> {
        Class<? extends TestExecutionListener> listenerClass = listener.getClass();
        if (uniqueListeners.stream().map(Object::getClass).noneMatch(listenerClass::equals)) {
          uniqueListeners.add(listener);
        }
      });
      listeners = uniqueListeners;

      // Sort by Ordered/@Order if we loaded default listeners.
      AnnotationAwareOrderComparator.sort(listeners);
    }

    if (log.isTraceEnabled()) {
      log.trace("Using TestExecutionListeners for test class [%s]: %s"
              .formatted(clazz.getName(), listeners));
    }
    else if (log.isDebugEnabled()) {
      log.debug("Using TestExecutionListeners for test class [%s]: %s"
              .formatted(clazz.getSimpleName(), classSimpleNames(listeners)));
    }
    return listeners;
  }

  @SuppressWarnings("unchecked")
  private List<TestExecutionListener> instantiateListeners(Class<? extends TestExecutionListener>... classes) {
    return instantiateComponents(TestExecutionListener.class, classes);
  }

  /**
   * Get the default {@link TestExecutionListener TestExecutionListeners} for
   * this bootstrapper.
   * <p>The default implementation delegates to
   * {@link TestContextFactoriesUtils#loadFactoryImplementations(Class)}.
   * <p>This method is invoked by {@link #getTestExecutionListeners()}.
   *
   * @return an <em>unmodifiable</em> list of default {@code TestExecutionListener}
   * instances
   */
  protected List<TestExecutionListener> getDefaultTestExecutionListeners() {
    return TestContextFactoriesUtils.loadFactoryImplementations(TestExecutionListener.class);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public final MergedContextConfiguration buildMergedContextConfiguration() {
    Class<?> testClass = getBootstrapContext().getTestClass();
    CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

    if (TestContextAnnotationUtils.findAnnotationDescriptorForTypes(
            testClass, ContextConfiguration.class, ContextHierarchy.class) == null) {
      return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
    }

    if (TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ContextHierarchy.class) != null) {
      Map<String, List<ContextConfigurationAttributes>> hierarchyMap =
              ContextLoaderUtils.buildContextHierarchyMap(testClass);
      MergedContextConfiguration parentConfig = null;
      MergedContextConfiguration mergedConfig = null;

      for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
        List<ContextConfigurationAttributes> reversedList = new ArrayList<>(list);
        Collections.reverse(reversedList);

        // Don't use the supplied testClass; instead ensure that we are
        // building the MCC for the actual test class that declared the
        // configuration for the current level in the context hierarchy.
        Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
        Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

        mergedConfig = buildMergedContextConfiguration(
                declaringClass, reversedList, parentConfig, cacheAwareContextLoaderDelegate, true);
        parentConfig = mergedConfig;
      }

      // Return the last level in the context hierarchy
      Assert.state(mergedConfig != null, "No merged context configuration");
      return mergedConfig;
    }
    else {
      return buildMergedContextConfiguration(testClass,
              ContextLoaderUtils.resolveContextConfigurationAttributes(testClass),
              null, cacheAwareContextLoaderDelegate, true);
    }
  }

  private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

    List<ContextConfigurationAttributes> defaultConfigAttributesList =
            Collections.singletonList(new ContextConfigurationAttributes(testClass));

    ContextLoader contextLoader = resolveContextLoader(testClass, defaultConfigAttributesList);
    if (log.isTraceEnabled()) {
      log.trace(String.format(
              "Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]: using %s",
              testClass.getName(), contextLoader.getClass().getName()));
    }
    else if (log.isDebugEnabled()) {
      log.debug(String.format(
              "Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]: using %s",
              testClass.getSimpleName(), contextLoader.getClass().getSimpleName()));
    }
    return buildMergedContextConfiguration(testClass, defaultConfigAttributesList, null,
            cacheAwareContextLoaderDelegate, false);
  }

  /**
   * Build the {@link MergedContextConfiguration merged context configuration}
   * for the supplied {@link Class testClass}, context configuration attributes,
   * and parent context configuration.
   *
   * @param testClass the test class for which the {@code MergedContextConfiguration}
   * should be built (must not be {@code null})
   * @param configAttributesList the list of context configuration attributes for the
   * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
   * traversing up the class hierarchy and enclosing class hierarchy); never
   * {@code null} or empty
   * @param parentConfig the merged context configuration for the parent application
   * context in a context hierarchy, or {@code null} if there is no parent
   * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
   * be passed to the {@code MergedContextConfiguration} constructor
   * @param requireLocationsClassesOrInitializers whether locations, classes, or
   * initializers are required; typically {@code true} but may be set to {@code false}
   * if the configured loader supports empty configuration
   * @return the merged context configuration
   * @see #resolveContextLoader
   * @see ContextLoaderUtils#resolveContextConfigurationAttributes
   * @see SmartContextLoader#processContextConfiguration
   * @see ContextLoader#processLocations
   * @see ActiveProfilesUtils#resolveActiveProfiles
   * @see ApplicationContextInitializerUtils#resolveInitializerClasses
   * @see MergedContextConfiguration
   */
  private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributesList, @Nullable MergedContextConfiguration parentConfig,
          CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
          boolean requireLocationsClassesOrInitializers) {

    Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

    ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
    List<String> locations = new ArrayList<>();
    List<Class<?>> classes = new ArrayList<>();
    List<Class<?>> initializers = new ArrayList<>();

    for (ContextConfigurationAttributes configAttributes : configAttributesList) {
      if (log.isTraceEnabled()) {
        log.trace(String.format("Processing locations and classes for context configuration attributes %s",
                configAttributes));
      }
      if (contextLoader instanceof SmartContextLoader smartContextLoader) {
        smartContextLoader.processContextConfiguration(configAttributes);
        locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
        classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
      }
      else {
        @SuppressWarnings("deprecation")
        String[] processedLocations = contextLoader.processLocations(
                configAttributes.getDeclaringClass(), configAttributes.getLocations());
        locations.addAll(0, Arrays.asList(processedLocations));
        // Legacy ContextLoaders don't know how to process classes
      }
      initializers.addAll(0, Arrays.asList(configAttributes.getInitializers()));
      if (!configAttributes.isInheritLocations()) {
        break;
      }
    }

    Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
            Collections.unmodifiableList(configAttributesList));

    Assert.state(!(requireLocationsClassesOrInitializers &&
            areAllEmpty(locations, classes, initializers, contextCustomizers)), () -> String.format(
            "%s was unable to detect defaults, and no ApplicationContextInitializers " +
                    "or ContextCustomizers were declared for context configuration attributes %s",
            contextLoader.getClass().getSimpleName(), configAttributesList));

    MergedTestPropertySources mergedTestPropertySources =
            TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
            StringUtils.toStringArray(locations), ClassUtils.toClassArray(classes),
            ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList),
            ActiveProfilesUtils.resolveActiveProfiles(testClass),
            mergedTestPropertySources.getPropertySourceDescriptors(),
            mergedTestPropertySources.getProperties(),
            contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

    return processMergedContextConfiguration(mergedConfig);
  }

  private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    List<ContextCustomizerFactory> factories = getContextCustomizerFactories(testClass);
    Set<ContextCustomizer> customizers = new LinkedHashSet<>(factories.size());
    for (ContextCustomizerFactory factory : factories) {
      ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
      if (customizer != null) {
        customizers.add(customizer);
      }
    }
    if (log.isTraceEnabled()) {
      log.trace("Using ContextCustomizers for test class [%s]: %s"
              .formatted(testClass.getName(), customizers));
    }
    else if (log.isDebugEnabled()) {
      log.debug("Using ContextCustomizers for test class [%s]: %s"
              .formatted(testClass.getSimpleName(), classSimpleNames(customizers)));
    }
    return customizers;
  }

  private List<ContextCustomizerFactory> getContextCustomizerFactories(Class<?> testClass) {
    AnnotationDescriptor<ContextCustomizerFactories> descriptor =
            TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ContextCustomizerFactories.class);
    List<ContextCustomizerFactory> factories = new ArrayList<>();

    if (descriptor == null) {
      if (log.isTraceEnabled()) {
        log.trace("@ContextCustomizerFactories is not present for class [%s]"
                .formatted(testClass.getName()));
      }
      factories.addAll(getContextCustomizerFactories());
    }
    else {
      // Traverse the class hierarchy...
      while (descriptor != null) {
        Class<?> declaringClass = descriptor.getDeclaringClass();
        ContextCustomizerFactories annotation = descriptor.getAnnotation();
        if (log.isTraceEnabled()) {
          log.trace("Retrieved %s for declaring class [%s]."
                  .formatted(annotation, declaringClass.getName()));
        }

        boolean inheritFactories = annotation.inheritFactories();
        AnnotationDescriptor<ContextCustomizerFactories> parentDescriptor = descriptor.next();
        factories.addAll(0, instantiateCustomizerFactories(annotation.factories()));

        // If there are no factories to inherit, we might need to merge the
        // locally declared factories with the defaults.
        if ((!inheritFactories || parentDescriptor == null) &&
                annotation.mergeMode() == ContextCustomizerFactories.MergeMode.MERGE_WITH_DEFAULTS) {
          if (log.isTraceEnabled()) {
            log.trace(String.format("Merging default factories with factories configured via " +
                    "@ContextCustomizerFactories for class [%s].", descriptor.getRootDeclaringClass().getName()));
          }
          factories.addAll(0, getContextCustomizerFactories());
        }

        descriptor = (inheritFactories ? parentDescriptor : null);
      }
    }

    // Remove possible duplicates.
    List<ContextCustomizerFactory> uniqueFactories = new ArrayList<>(factories.size());
    factories.forEach(factory -> {
      Class<? extends ContextCustomizerFactory> factoryClass = factory.getClass();
      if (uniqueFactories.stream().map(Object::getClass).noneMatch(factoryClass::equals)) {
        uniqueFactories.add(factory);
      }
    });
    factories = uniqueFactories;

    if (log.isTraceEnabled()) {
      log.trace("Using ContextCustomizerFactory implementations for test class [%s]: %s"
              .formatted(testClass.getName(), factories));
    }
    else if (log.isDebugEnabled()) {
      log.debug("Using ContextCustomizerFactory implementations for test class [%s]: %s"
              .formatted(testClass.getSimpleName(), classSimpleNames(factories)));
    }

    return factories;
  }

  /**
   * Get the {@link ContextCustomizerFactory} instances for this bootstrapper.
   * <p>The default implementation delegates to
   * {@link TestContextFactoriesUtils#loadFactoryImplementations(Class)}.
   */
  protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
    return TestContextFactoriesUtils.loadFactoryImplementations(ContextCustomizerFactory.class);
  }

  @SuppressWarnings("unchecked")
  private List<ContextCustomizerFactory> instantiateCustomizerFactories(Class<? extends ContextCustomizerFactory>... classes) {
    return instantiateComponents(ContextCustomizerFactory.class, classes);
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> instantiateComponents(Class<T> componentType, Class<? extends T>... classes) {
    List<T> components = new ArrayList<>(classes.length);
    for (Class<? extends T> clazz : classes) {
      try {
        components.add(BeanUtils.newInstance(clazz));
      }
      catch (BeanInstantiationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
          if (log.isDebugEnabled()) {
            log.debug("""
                    Skipping candidate %1$s [%2$s] due to a missing dependency. \
                    Specify custom %1$s classes or make the default %1$s classes \
                    and their required dependencies available. Offending class: [%3$s]"""
                    .formatted(componentType.getSimpleName(), clazz.getName(), cause.getMessage()));
          }
        }
        else {
          throw ex;
        }
      }
    }
    return components;
  }

  /**
   * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the
   * supplied list of {@link ContextConfigurationAttributes} and then instantiate
   * and return that {@code ContextLoader}.
   * <p>If the user has not explicitly declared which loader to use, the value
   * returned from {@link #getDefaultContextLoaderClass} will be used as the
   * default context loader class. For details on the class resolution process,
   * see {@link #resolveExplicitContextLoaderClass} and
   * {@link #getDefaultContextLoaderClass}.
   *
   * @param testClass the test class for which the {@code ContextLoader} should be
   * resolved; must not be {@code null}
   * @param configAttributesList the list of configuration attributes to process; must
   * not be {@code null}; must be ordered <em>bottom-up</em>
   * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
   * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
   * (never {@code null})
   * @throws IllegalStateException if {@link #getDefaultContextLoaderClass(Class)}
   * returns {@code null}
   */
  protected ContextLoader resolveContextLoader(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributesList) {

    Assert.notNull(testClass, "Class must not be null");
    Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

    Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
    if (contextLoaderClass == null) {
      contextLoaderClass = getDefaultContextLoaderClass(testClass);
    }
    if (log.isTraceEnabled()) {
      log.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
              contextLoaderClass.getName(), testClass.getName()));
    }
    return BeanUtils.newInstance(contextLoaderClass);
  }

  /**
   * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
   * list of {@link ContextConfigurationAttributes}.
   * <p>Beginning with the first level in the context configuration attributes hierarchy:
   * <ol>
   * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
   * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
   * configured with an explicit class, that class will be returned.</li>
   * <li>If an explicit {@code ContextLoader} class is not specified at the current
   * level in the hierarchy, traverse to the next level in the hierarchy and return to
   * step #1.</li>
   * </ol>
   *
   * @param configAttributesList the list of configuration attributes to process;
   * must not be {@code null}; must be ordered <em>bottom-up</em>
   * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
   * @return the {@code ContextLoader} class to use for the supplied configuration
   * attributes, or {@code null} if no explicit loader is found
   * @throws IllegalArgumentException if supplied configuration attributes are
   * {@code null} or <em>empty</em>
   */
  @Nullable
  protected Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
          List<ContextConfigurationAttributes> configAttributesList) {

    Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

    for (ContextConfigurationAttributes configAttributes : configAttributesList) {
      if (log.isTraceEnabled()) {
        log.trace("Resolving ContextLoader for context configuration attributes " + configAttributes);
      }
      Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
      if (ContextLoader.class != contextLoaderClass) {
        if (log.isTraceEnabled()) {
          log.trace("Found explicit ContextLoader class [%s] for context configuration attributes %s"
                  .formatted(contextLoaderClass.getName(), configAttributes));
        }
        else if (log.isDebugEnabled()) {
          log.debug("Found explicit ContextLoader class [%s] for test class [%s]"
                  .formatted(contextLoaderClass.getSimpleName(), configAttributes.getDeclaringClass().getSimpleName()));
        }
        return contextLoaderClass;
      }
    }
    return null;
  }

  /**
   * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
   * interaction with the {@code ContextCache}.
   * <p>The default implementation delegates to
   * {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()} and
   * the default one will load {@link cn.taketoday.test.context.ApplicationContextFailureProcessor}
   * via the service loading mechanism.
   * <p>Concrete subclasses may choose to override this method to return a custom
   * {@code CacheAwareContextLoaderDelegate} implementation with custom
   * {@link cn.taketoday.test.context.cache.ContextCache ContextCache} support.
   *
   * @return the context loader delegate (never {@code null})
   * @see cn.taketoday.test.context.ApplicationContextFailureProcessor
   */
  protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
    return getBootstrapContext().getCacheAwareContextLoaderDelegate();
  }

  /**
   * Determine the default {@link ContextLoader} {@linkplain Class class}
   * to use for the supplied test class.
   * <p>The class returned by this method will only be used if a {@code ContextLoader}
   * class has not been explicitly declared via {@link ContextConfiguration#loader}.
   *
   * @param testClass the test class for which to retrieve the default
   * {@code ContextLoader} class
   * @return the default {@code ContextLoader} class for the supplied test class
   * (never {@code null})
   */
  protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

  /**
   * Process the supplied, newly instantiated {@link MergedContextConfiguration} instance.
   * <p>The returned {@link MergedContextConfiguration} instance may be a wrapper
   * around or a replacement for the original.
   * <p>The default implementation simply returns the supplied instance unmodified.
   * <p>Concrete subclasses may choose to return a specialized subclass of
   * {@link MergedContextConfiguration} based on properties in the supplied instance.
   *
   * @param mergedConfig the {@code MergedContextConfiguration} to process; never {@code null}
   * @return a fully initialized {@code MergedContextConfiguration}; never {@code null}
   */
  protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    return mergedConfig;
  }

  private static List<String> classSimpleNames(Collection<?> components) {
    return components.stream().map(Object::getClass).map(Class::getSimpleName).toList();
  }

  private static boolean areAllEmpty(Collection<?>... collections) {
    return Arrays.stream(collections).allMatch(Collection::isEmpty);
  }

}
