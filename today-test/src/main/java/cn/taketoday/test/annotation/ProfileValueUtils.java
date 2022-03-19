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

package cn.taketoday.test.annotation;

import java.lang.reflect.Method;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * General utility methods for working with <em>profile values</em>.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see ProfileValueSource
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 * @since 4.0
 */
public abstract class ProfileValueUtils {

  private static final Logger logger = LoggerFactory.getLogger(ProfileValueUtils.class);

  /**
   * Retrieves the {@link ProfileValueSource} type for the specified
   * {@link Class test class} as configured via the
   * {@link ProfileValueSourceConfiguration
   * &#064;ProfileValueSourceConfiguration} annotation and instantiates a new
   * instance of that type.
   * <p>If {@link ProfileValueSourceConfiguration
   * &#064;ProfileValueSourceConfiguration} is not present on the specified
   * class or if a custom {@link ProfileValueSource} is not declared, the
   * default {@link SystemProfileValueSource} will be returned instead.
   *
   * @param testClass the test class for which the ProfileValueSource should
   * be retrieved
   * @return the configured (or default) ProfileValueSource for the specified
   * class
   * @see SystemProfileValueSource
   */
  @SuppressWarnings("unchecked")
  public static ProfileValueSource retrieveProfileValueSource(Class<?> testClass) {
    Assert.notNull(testClass, "testClass must not be null");

    Class<ProfileValueSourceConfiguration> annotationType = ProfileValueSourceConfiguration.class;
    ProfileValueSourceConfiguration config = AnnotatedElementUtils.findMergedAnnotation(testClass, annotationType);
    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved @ProfileValueSourceConfiguration [" + config + "] for test class [" +
              testClass.getName() + "]");
    }

    Class<? extends ProfileValueSource> profileValueSourceType;
    if (config != null) {
      profileValueSourceType = config.value();
    }
    else {
      profileValueSourceType = (Class<? extends ProfileValueSource>) AnnotationUtils.getDefaultValue(annotationType);
      Assert.state(profileValueSourceType != null, "No default ProfileValueSource class");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved ProfileValueSource type [" + profileValueSourceType + "] for class [" +
              testClass.getName() + "]");
    }

    ProfileValueSource profileValueSource;
    if (SystemProfileValueSource.class == profileValueSourceType) {
      profileValueSource = SystemProfileValueSource.getInstance();
    }
    else {
      try {
        profileValueSource = ReflectionUtils.accessibleConstructor(profileValueSourceType).newInstance();
      }
      catch (Exception ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not instantiate a ProfileValueSource of type [" + profileValueSourceType +
                  "] for class [" + testClass.getName() + "]: using default.", ex);
        }
        profileValueSource = SystemProfileValueSource.getInstance();
      }
    }

    return profileValueSource;
  }

  /**
   * Determine if the supplied {@code testClass} is <em>enabled</em> in
   * the current environment, as specified by the {@link IfProfileValue
   * &#064;IfProfileValue} annotation at the class level.
   * <p>Defaults to {@code true} if no {@link IfProfileValue
   * &#064;IfProfileValue} annotation is declared.
   *
   * @param testClass the test class
   * @return {@code true} if the test is <em>enabled</em> in the current
   * environment
   */
  public static boolean isTestEnabledInThisEnvironment(Class<?> testClass) {
    IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
    return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), ifProfileValue);
  }

  /**
   * Determine if the supplied {@code testMethod} is <em>enabled</em> in
   * the current environment, as specified by the {@link IfProfileValue
   * &#064;IfProfileValue} annotation, which may be declared on the test
   * method itself or at the class level. Class-level usage overrides
   * method-level usage.
   * <p>Defaults to {@code true} if no {@link IfProfileValue
   * &#064;IfProfileValue} annotation is declared.
   *
   * @param testMethod the test method
   * @param testClass the test class
   * @return {@code true} if the test is <em>enabled</em> in the current
   * environment
   */
  public static boolean isTestEnabledInThisEnvironment(Method testMethod, Class<?> testClass) {
    return isTestEnabledInThisEnvironment(retrieveProfileValueSource(testClass), testMethod, testClass);
  }

  /**
   * Determine if the supplied {@code testMethod} is <em>enabled</em> in
   * the current environment, as specified by the {@link IfProfileValue
   * &#064;IfProfileValue} annotation, which may be declared on the test
   * method itself or at the class level. Class-level usage overrides
   * method-level usage.
   * <p>Defaults to {@code true} if no {@link IfProfileValue
   * &#064;IfProfileValue} annotation is declared.
   *
   * @param profileValueSource the ProfileValueSource to use to determine if
   * the test is enabled
   * @param testMethod the test method
   * @param testClass the test class
   * @return {@code true} if the test is <em>enabled</em> in the current
   * environment
   */
  public static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource, Method testMethod,
          Class<?> testClass) {

    IfProfileValue ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testClass, IfProfileValue.class);
    boolean classLevelEnabled = isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);

    if (classLevelEnabled) {
      ifProfileValue = AnnotatedElementUtils.findMergedAnnotation(testMethod, IfProfileValue.class);
      return isTestEnabledInThisEnvironment(profileValueSource, ifProfileValue);
    }

    return false;
  }

  /**
   * Determine if the {@code value} (or one of the {@code values})
   * in the supplied {@link IfProfileValue &#064;IfProfileValue} annotation is
   * <em>enabled</em> in the current environment.
   *
   * @param profileValueSource the ProfileValueSource to use to determine if
   * the test is enabled
   * @param ifProfileValue the annotation to introspect; may be
   * {@code null}
   * @return {@code true} if the test is <em>enabled</em> in the current
   * environment or if the supplied {@code ifProfileValue} is
   * {@code null}
   */
  private static boolean isTestEnabledInThisEnvironment(ProfileValueSource profileValueSource,
          @Nullable IfProfileValue ifProfileValue) {

    if (ifProfileValue == null) {
      return true;
    }

    String environmentValue = profileValueSource.get(ifProfileValue.name());
    String[] annotatedValues = ifProfileValue.values();
    if (StringUtils.isNotEmpty(ifProfileValue.value())) {
      Assert.isTrue(annotatedValues.length == 0, () -> "Setting both the 'value' and 'values' attributes " +
              "of @IfProfileValue is not allowed: choose one or the other.");
      annotatedValues = new String[] { ifProfileValue.value() };
    }

    for (String value : annotatedValues) {
      if (ObjectUtils.nullSafeEquals(value, environmentValue)) {
        return true;
      }
    }
    return false;
  }

}
