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

package cn.taketoday.test.context.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.SpringProperties;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.TestConstructor;
import cn.taketoday.test.context.TestConstructor.AutowireMode;
import cn.taketoday.test.context.TestContextAnnotationUtils;

/**
 * Utility methods for working with {@link TestConstructor @TestConstructor}.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @see TestConstructor
 * @since 5.2
 */
public abstract class TestConstructorUtils {

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
   * if no such value is found in {@link SpringProperties}
   * @return {@code true} if the executable is an autowirable constructor
   * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
   * @since 5.3
   */
  public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass,
          @Nullable PropertyProvider fallbackPropertyProvider) {

    return (executable instanceof Constructor &&
            isAutowirableConstructor((Constructor<?>) executable, testClass, fallbackPropertyProvider));
  }

  /**
   * Determine if the supplied constructor for the given test class is
   * autowirable.
   *
   * <p>A constructor is considered to be autowirable if one of the following
   * conditions is {@code true}.
   *
   * <ol>
   * <li>The constructor is annotated with {@link Autowired @Autowired}.</li>
   * <li>{@link TestConstructor @TestConstructor} is <em>present</em> or
   * <em>meta-present</em> on the test class with
   * {@link TestConstructor#autowireMode() autowireMode} set to
   * {@link AutowireMode#ALL ALL}.</li>
   * <li>The default <em>test constructor autowire mode</em> has been set to
   * {@code ALL} in {@link SpringProperties} or in the supplied fallback
   * {@link PropertyProvider} (see
   * {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}).</li>
   * </ol>
   *
   * @param constructor a constructor for the test class
   * @param testClass the test class
   * @param fallbackPropertyProvider fallback property provider used to look up
   * the value for the default <em>test constructor autowire mode</em> if no
   * such value is found in {@link SpringProperties}
   * @return {@code true} if the constructor is autowirable
   * @since 5.3
   */
  public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass,
          @Nullable PropertyProvider fallbackPropertyProvider) {

    // Is the constructor annotated with @Autowired?
    if (AnnotatedElementUtils.hasAnnotation(constructor, Autowired.class)) {
      return true;
    }

    AutowireMode autowireMode = null;

    // Is the test class annotated with @TestConstructor?
    TestConstructor testConstructor = TestContextAnnotationUtils.findMergedAnnotation(testClass, TestConstructor.class);
    if (testConstructor != null) {
      autowireMode = testConstructor.autowireMode();
    }
    else {
      // Custom global default from SpringProperties?
      String value = SpringProperties.getProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
      autowireMode = AutowireMode.from(value);

      // Use fallback provider?
      if (autowireMode == null && fallbackPropertyProvider != null) {
        value = fallbackPropertyProvider.get(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
        autowireMode = AutowireMode.from(value);
      }
    }

    return (autowireMode == AutowireMode.ALL);
  }

}
