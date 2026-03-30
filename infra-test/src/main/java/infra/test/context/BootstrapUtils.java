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

package infra.test.context;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import infra.beans.BeanUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotationPredicates;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>TestContext Framework</em>.
 *
 * <p>Only intended for internal use.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @since 4.0
 */
public abstract class BootstrapUtils {

  private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
          "infra.test.context.support.DefaultBootstrapContext";

  private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
          "infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

  private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
          "infra.test.context.support.DefaultTestContextBootstrapper";

  private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
          "infra.test.context.web.WebTestContextBootstrapper";

  private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
          "infra.test.context.web.WebAppConfiguration";

  private static final Class<? extends Annotation> WEB_APP_CONFIGURATION_CLASS = loadWebAppConfigurationClass();

  private static final Logger log = LoggerFactory.getLogger(BootstrapUtils.class);

  /**
   * Create the {@code BootstrapContext} for the specified {@linkplain Class test class}.
   * <p>Uses reflection to create a {@link infra.test.context.support.DefaultBootstrapContext}.
   * that uses a {@link infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate}.
   *
   * @param testClass the test class for which the bootstrap context should be created
   * @return a new {@code BootstrapContext}; never {@code null}
   */
  static BootstrapContext createBootstrapContext(Class<?> testClass) {
    CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
    String className = DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME;
    Class<? extends BootstrapContext> clazz;
    try {
      clazz = ClassUtils.forName(className, BootstrapUtils.class.getClassLoader());
      Constructor<? extends BootstrapContext> constructor =
              clazz.getConstructor(Class.class, CacheAwareContextLoaderDelegate.class);
      log.trace("Instantiating BootstrapContext using constructor [{}]", constructor);
      return BeanUtils.newInstance(constructor, testClass, cacheAwareContextLoaderDelegate);
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not load BootstrapContext [%s]".formatted(className), ex);
    }
  }

  private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
    String className = DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME;
    Class<? extends CacheAwareContextLoaderDelegate> clazz;
    try {
      clazz = ClassUtils.forName(className, BootstrapUtils.class.getClassLoader());
      log.trace("Instantiating CacheAwareContextLoaderDelegate from class [{}]", className);
      return BeanUtils.newInstance(clazz);
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not load CacheAwareContextLoaderDelegate [%s]".formatted(className), ex);
    }
  }

  /**
   * Resolve the {@link TestContextBootstrapper} type for the supplied test class
   * using the default {@link BootstrapContext}, instantiate the bootstrapper,
   * and provide it a reference to the {@code BootstrapContext}.
   * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
   * the test class, either directly or as a meta-annotation, then its
   * {@link BootstrapWith#value value} will be used as the bootstrapper type.
   * Otherwise, either the
   * {@link infra.test.context.support.DefaultTestContextBootstrapper
   * DefaultTestContextBootstrapper} or the
   * {@link infra.test.context.web.WebTestContextBootstrapper
   * WebTestContextBootstrapper} will be used, depending on the presence of
   * {@link infra.test.context.web.WebAppConfiguration @WebAppConfiguration}.
   *
   * @param testClass the test class for which the bootstrapper should be created
   * @return a fully configured {@code TestContextBootstrapper}
   */
  public static TestContextBootstrapper resolveTestContextBootstrapper(Class<?> testClass) {
    return resolveTestContextBootstrapper(createBootstrapContext(testClass));
  }

  /**
   * Resolve the {@link TestContextBootstrapper} type for the test class in the
   * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
   * to the {@link BootstrapContext}.
   * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
   * the test class, either directly or as a meta-annotation, then its
   * {@link BootstrapWith#value value} will be used as the bootstrapper type.
   * Otherwise, either the
   * {@link infra.test.context.support.DefaultTestContextBootstrapper
   * DefaultTestContextBootstrapper} or the
   * {@link infra.test.context.web.WebTestContextBootstrapper
   * WebTestContextBootstrapper} will be used, depending on the presence of
   * {@link infra.test.context.web.WebAppConfiguration @WebAppConfiguration}.
   *
   * @param bootstrapContext the bootstrap context to use
   * @return a fully configured {@code TestContextBootstrapper}
   */
  static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
    Class<?> testClass = bootstrapContext.getTestClass();

    Class<?> clazz = null;
    try {
      clazz = resolveExplicitTestContextBootstrapper(testClass);
      if (clazz == null) {
        clazz = resolveDefaultTestContextBootstrapper(testClass);
      }
      log.trace("Instantiating TestContextBootstrapper for test class [{}] from class [{}]",
              testClass.getName(), clazz.getName());
      TestContextBootstrapper testContextBootstrapper = (TestContextBootstrapper) BeanUtils.newInstance(clazz);
      testContextBootstrapper.setBootstrapContext(bootstrapContext);
      return testContextBootstrapper;
    }
    catch (IllegalStateException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new IllegalStateException("""
              Could not load TestContextBootstrapper [%s]. Specify @BootstrapWith's 'value' \
              attribute or make the default bootstrapper class available.""".formatted(clazz), ex);
    }
  }

  private static @Nullable Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
    Map<Integer, Set<BootstrapWith>> distanceToAnnotationsMap = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
            .withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
            .from(testClass)
            .stream(BootstrapWith.class)
            // The following effectively filters out annotations in the type and
            // enclosing class hierarchies once annotations have already been found
            // on a particular class or interface.
            .filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
            // Grouping by "meta-distance" enables us to allow a directly-present
            // annotation to override annotations that are meta-present.
            // Collecting synthesized annotations for each meta-distance enables
            // us to filter out duplicates.
            .collect(Collectors.groupingBy(MergedAnnotation::getDistance, TreeMap::new,
                    Collectors.mapping(MergedAnnotation::synthesize, Collectors.toCollection(LinkedHashSet::new))));

    if (distanceToAnnotationsMap.isEmpty()) {
      return null;
    }

    Set<BootstrapWith> annotations = new LinkedHashSet<>();
    for (Set<BootstrapWith> currentAnnotations : distanceToAnnotationsMap.values()) {
      // If we have found a single, non-competing @BootstrapWith annotation, return it.
      if (annotations.isEmpty() && currentAnnotations.size() == 1) {
        return currentAnnotations.iterator().next().value();
      }
      // Otherwise, track all discovered annotations for error reporting.
      annotations.addAll(currentAnnotations);
    }

    throw new IllegalStateException(String.format(
            "Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
            testClass.getName(), annotations));
  }

  private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
    boolean webApp = TestContextAnnotationUtils.hasAnnotation(testClass, WEB_APP_CONFIGURATION_CLASS);
    String bootstrapperClassName = (webApp ? DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME :
            DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME);
    return ClassUtils.forName(bootstrapperClassName, BootstrapUtils.class.getClassLoader());
  }

  private static Class<? extends Annotation> loadWebAppConfigurationClass() {
    try {
      return ClassUtils.forName(WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME,
              BootstrapUtils.class.getClassLoader());
    }
    catch (ClassNotFoundException | LinkageError ex) {
      throw new IllegalStateException(
              "Failed to load class for @" + WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, ex);
    }
  }

}
