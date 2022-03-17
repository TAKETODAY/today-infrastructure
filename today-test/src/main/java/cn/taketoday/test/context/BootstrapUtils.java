/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.SpringProperties;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.support.DefaultBootstrapContext;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.context.web.WebTestContextBootstrapper;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @since 4.1
 */
abstract class BootstrapUtils {

  private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
          "cn.taketoday.test.context.support.DefaultBootstrapContext";

  private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
          "cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

  private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
          "cn.taketoday.test.context.support.DefaultTestContextBootstrapper";

  private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
          "cn.taketoday.test.context.web.WebTestContextBootstrapper";

  private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
          "cn.taketoday.test.context.web.WebAppConfiguration";

  private static final Class<? extends Annotation> webAppConfigurationClass = loadWebAppConfigurationClass();

  private static final Log logger = LogFactory.getLog(BootstrapUtils.class);

  /**
   * Create the {@code BootstrapContext} for the specified {@linkplain Class test class}.
   * <p>Uses reflection to create a {@link DefaultBootstrapContext}
   * that uses a default {@link CacheAwareContextLoaderDelegate} &mdash; configured
   * via the {@link CacheAwareContextLoaderDelegate#DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME}
   * system property or falling back to the
   * {@link DefaultCacheAwareContextLoaderDelegate}
   * if the system property is not defined.
   *
   * @param testClass the test class for which the bootstrap context should be created
   * @return a new {@code BootstrapContext}; never {@code null}
   */
  @SuppressWarnings("unchecked")
  static BootstrapContext createBootstrapContext(Class<?> testClass) {
    CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
    Class<? extends BootstrapContext> clazz = null;
    try {
      clazz = ClassUtils.forName(
              DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME, BootstrapUtils.class.getClassLoader());
      Constructor<? extends BootstrapContext> constructor = clazz.getConstructor(
              Class.class, CacheAwareContextLoaderDelegate.class);
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Instantiating BootstrapContext using constructor [%s]", constructor));
      }
      return BeanUtils.instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not load BootstrapContext [" + clazz + "]", ex);
    }
  }

  @SuppressWarnings("unchecked")
  private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
    String className = SpringProperties.getProperty(
            CacheAwareContextLoaderDelegate.DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME);
    className = (StringUtils.hasText(className) ? className.trim() :
                 DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME);
    try {
      Class<? extends CacheAwareContextLoaderDelegate> clazz =
              ClassUtils.forName(
                      className, BootstrapUtils.class.getClassLoader());
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]", className));
      }
      return BeanUtils.instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not create CacheAwareContextLoaderDelegate [" + className + "]", ex);
    }
  }

  /**
   * Resolve the {@link TestContextBootstrapper} type for the test class in the
   * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
   * to the {@link BootstrapContext}.
   * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
   * the test class, either directly or as a meta-annotation, then its
   * {@link BootstrapWith#value value} will be used as the bootstrapper type.
   * Otherwise, either the
   * {@link DefaultTestContextBootstrapper
   * DefaultTestContextBootstrapper} or the
   * {@link WebTestContextBootstrapper
   * WebTestContextBootstrapper} will be used, depending on the presence of
   * {@link WebAppConfiguration @WebAppConfiguration}.
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
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
                testClass.getName(), clazz.getName()));
      }
      TestContextBootstrapper testContextBootstrapper =
              BeanUtils.instantiateClass(clazz, TestContextBootstrapper.class);
      testContextBootstrapper.setBootstrapContext(bootstrapContext);
      return testContextBootstrapper;
    }
    catch (IllegalStateException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz +
              "]. Specify @BootstrapWith's 'value' attribute or make the default bootstrapper class available.",
              ex);
    }
  }

  @Nullable
  private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
    Set<BootstrapWith> annotations = new LinkedHashSet<>();
    AnnotationDescriptor<BootstrapWith> descriptor =
            TestContextAnnotationUtils.findAnnotationDescriptor(testClass, BootstrapWith.class);
    while (descriptor != null) {
      annotations.addAll(descriptor.findAllLocalMergedAnnotations());
      descriptor = descriptor.next();
    }

    if (annotations.isEmpty()) {
      return null;
    }
    if (annotations.size() == 1) {
      return annotations.iterator().next().value();
    }

    // Allow directly-present annotation to override annotations that are meta-present.
    BootstrapWith bootstrapWith = testClass.getDeclaredAnnotation(BootstrapWith.class);
    if (bootstrapWith != null) {
      return bootstrapWith.value();
    }

    throw new IllegalStateException(String.format(
            "Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
            testClass.getName(), annotations));
  }

  private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
    boolean webApp = TestContextAnnotationUtils.hasAnnotation(testClass, webAppConfigurationClass);
    String bootstrapperClassName = (webApp ? DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME :
                                    DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME);
    return ClassUtils.forName(bootstrapperClassName, BootstrapUtils.class.getClassLoader());
  }

  @SuppressWarnings("unchecked")
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
