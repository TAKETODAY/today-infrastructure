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

package infra.test.context.support;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.annotation.Autowired;
import infra.core.annotation.AnnotatedElementUtils;
import infra.util.InfraStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.TestConstructor;
import infra.test.context.TestContextAnnotationUtils;
import infra.util.ClassUtils;

/**
 * Utility methods for working with {@link TestConstructor @TestConstructor}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestConstructor
 * @since 4.0
 */
public abstract class TestConstructorUtils {

  private static final Logger logger = LoggerFactory.getLogger(TestConstructorUtils.class);

  private static final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(2);

  static {
    autowiredAnnotationTypes.add(Autowired.class);

    ClassLoader classLoader = TestConstructorUtils.class.getClassLoader();
    try {
      autowiredAnnotationTypes.add(ClassUtils.forName("jakarta.inject.Inject", classLoader));
      logger.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // jakarta.inject API not available - simply skip.
    }

    try {
      autowiredAnnotationTypes.add(ClassUtils.forName("javax.inject.Inject", classLoader));
      logger.trace("'javax.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // javax.inject API not available - simply skip.
    }
  }

  private TestConstructorUtils() {
  }

  /**
   * Determine if the supplied {@link Executable} is an autowirable {@link Constructor}.
   *
   * <p>A constructor is considered to be autowirable if one of the following
   * conditions is {@code true}.
   *
   * <ol>
   * <li>The constructor is annotated with {@link Autowired @Autowired} or
   * {@link jakarta.inject.Inject @jakarta.inject.Inject}.</li>
   * <li>{@link TestConstructor @TestConstructor} is <em>present</em> or
   * <em>meta-present</em> on the test class with
   * {@link TestConstructor#autowireMode() autowireMode} set to
   * {@link TestConstructor.AutowireMode#ALL ALL}.</li>
   * <li>The default <em>test constructor autowire mode</em> has been set to
   * {@code ALL} in {@link InfraStrategies} or in the supplied fallback
   * {@link PropertyProvider}.</li>
   * </ol>
   *
   * @param executable an {@code Executable} for a test class
   * @param fallbackPropertyProvider fallback property provider used to look up
   * the value for {@value TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}
   * if no such value is found in {@link InfraStrategies}; may be {@code null}
   * if there is no fallback support
   * @return {@code true} if the executable is an autowirable constructor
   * @see TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
   */
  public static boolean isAutowirableConstructor(Executable executable,
          @Nullable PropertyProvider fallbackPropertyProvider) {

    if (!(executable instanceof Constructor<?> constructor)) {
      return false;
    }

    // Is the constructor annotated with @Autowired/@Inject?
    if (isAnnotatedWithAutowiredOrInject(constructor)) {
      return true;
    }

    TestConstructor.AutowireMode autowireMode = null;

    // Is the test class annotated with @TestConstructor?
    Class<?> testClass = constructor.getDeclaringClass();
    TestConstructor testConstructor = TestContextAnnotationUtils.findMergedAnnotation(testClass, TestConstructor.class);
    if (testConstructor != null) {
      autowireMode = testConstructor.autowireMode();
    }
    else {
      // Custom global default from InfraStrategies?
      String value = InfraStrategies.getProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
      autowireMode = TestConstructor.AutowireMode.from(value);

      // Use fallback provider?
      if (autowireMode == null && fallbackPropertyProvider != null) {
        value = fallbackPropertyProvider.get(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
        autowireMode = TestConstructor.AutowireMode.from(value);
      }
    }

    return (autowireMode == TestConstructor.AutowireMode.ALL);
  }

  private static boolean isAnnotatedWithAutowiredOrInject(Constructor<?> constructor) {
    return autowiredAnnotationTypes.stream()
            .anyMatch(annotationType -> AnnotatedElementUtils.hasAnnotation(constructor, annotationType));
  }

}
