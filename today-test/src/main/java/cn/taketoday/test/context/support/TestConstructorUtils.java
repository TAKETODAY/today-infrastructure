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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.TestConstructor;
import cn.taketoday.test.context.TestConstructor.AutowireMode;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.util.ClassUtils;

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
   * Determine if the supplied executable for the given test class is an
   * autowirable constructor.
   * <p>This method delegates to {@link #isAutowirableConstructor(Executable, Class, PropertyProvider)}
   * will a value of {@code null} for the fallback {@link PropertyProvider}.
   *
   * @param executable an executable for the test class
   * @param testClass the test class
   * @return {@code true} if the executable is an autowirable constructor
   * @see #isAutowirableConstructor(Executable, Class, PropertyProvider)
   */
  public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass) {
    return isAutowirableConstructor(executable, testClass, null);
  }

  /**
   * Determine if the supplied constructor for the given test class is
   * autowirable.
   * <p>This method delegates to {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}
   * will a value of {@code null} for the fallback {@link PropertyProvider}.
   *
   * @param constructor a constructor for the test class
   * @param testClass the test class
   * @return {@code true} if the constructor is autowirable
   * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
   */
  public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass) {
    return isAutowirableConstructor(constructor, testClass, null);
  }

  /**
   * Determine if the supplied executable for the given test class is an
   * autowirable constructor.
   * <p>This method delegates to {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}
   * if the supplied executable is a constructor and otherwise returns {@code false}.
   *
   * @param executable an executable for the test class
   * @param testClass the test class
   * @param fallbackPropertyProvider fallback property provider used to look up
   * the value for {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}
   * if no such value is found in {@link TodayStrategies}
   * @return {@code true} if the executable is an autowirable constructor
   * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
   */
  public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass,
          @Nullable PropertyProvider fallbackPropertyProvider) {

    return (executable instanceof Constructor<?> constructor &&
            isAutowirableConstructor(constructor, testClass, fallbackPropertyProvider));
  }

  /**
   * Determine if the supplied constructor for the given test class is
   * autowirable.
   *
   * <p>A constructor is considered to be autowirable if one of the following
   * conditions is {@code true}.
   *
   * <ol>
   * <li>The constructor is annotated with {@link Autowired @Autowired},
   * {@link jakarta.inject.Inject @jakarta.inject.Inject}, or
   * {@link javax.inject.Inject @javax.inject.Inject}.</li>
   * <li>{@link TestConstructor @TestConstructor} is <em>present</em> or
   * <em>meta-present</em> on the test class with
   * {@link TestConstructor#autowireMode() autowireMode} set to
   * {@link AutowireMode#ALL ALL}.</li>
   * <li>The default <em>test constructor autowire mode</em> has been set to
   * {@code ALL} in {@link TodayStrategies} or in the supplied fallback
   * {@link PropertyProvider} (see
   * {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}).</li>
   * </ol>
   *
   * @param constructor a constructor for the test class
   * @param testClass the test class
   * @param fallbackPropertyProvider fallback property provider used to look up
   * the value for the default <em>test constructor autowire mode</em> if no
   * such value is found in {@link TodayStrategies}
   * @return {@code true} if the constructor is autowirable
   */
  public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass,
          @Nullable PropertyProvider fallbackPropertyProvider) {

    // Is the constructor annotated with @Autowired/@Inject?
    if (isAnnotatedWithAutowiredOrInject(constructor)) {
      return true;
    }

    AutowireMode autowireMode = null;

    // Is the test class annotated with @TestConstructor?
    TestConstructor testConstructor = TestContextAnnotationUtils.findMergedAnnotation(testClass, TestConstructor.class);
    if (testConstructor != null) {
      autowireMode = testConstructor.autowireMode();
    }
    else {
      // Custom global default from TodayStrategies?
      String value = TodayStrategies.getProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
      autowireMode = AutowireMode.from(value);

      // Use fallback provider?
      if (autowireMode == null && fallbackPropertyProvider != null) {
        value = fallbackPropertyProvider.get(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
        autowireMode = AutowireMode.from(value);
      }
    }

    return (autowireMode == AutowireMode.ALL);
  }

  private static boolean isAnnotatedWithAutowiredOrInject(Constructor<?> constructor) {
    return autowiredAnnotationTypes.stream()
            .anyMatch(annotationType -> AnnotatedElementUtils.hasAnnotation(constructor, annotationType));
  }

}
