/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import infra.context.annotation.Configuration;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.SmartContextLoader;
import infra.util.ClassUtils;

/**
 * Utility methods for {@link SmartContextLoader SmartContextLoaders} that deal
 * with component classes (e.g., {@link Configuration @Configuration} classes).
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class AnnotationConfigContextLoaderUtils {

  private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigContextLoaderUtils.class);

  /**
   * Detect the default configuration classes for the supplied test class.
   * <p>The returned class array will contain all static nested classes of
   * the supplied class that meet the requirements for {@code @Configuration}
   * class implementations as specified in the documentation for
   * {@link Configuration @Configuration}.
   * <p>The implementation of this method adheres to the contract defined in the
   * {@link SmartContextLoader SmartContextLoader}
   * SPI. Specifically, this method uses introspection to detect default
   * configuration classes that comply with the constraints required of
   * {@code @Configuration} class implementations. If a potential candidate
   * configuration class does not meet these requirements, this method will log a
   * debug message, and the potential candidate class will be ignored.
   *
   * @param declaringClass the test class that declared {@code @ContextConfiguration}
   * @return an array of default configuration classes, potentially empty but
   * never {@code null}
   */
  public static Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
    Assert.notNull(declaringClass, "Declaring class is required");

    List<Class<?>> configClasses = new ArrayList<>();

    for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
      if (isDefaultConfigurationClassCandidate(candidate)) {
        configClasses.add(candidate);
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(
                  "Ignoring class [%s]; it must be static, non-private, non-final, and annotated " +
                          "with @Configuration to be considered a default configuration class.",
                  candidate.getName()));
        }
      }
    }

    if (configClasses.isEmpty()) {
      if (logger.isInfoEnabled()) {
        logger.info(String.format("Could not detect default configuration classes for test class [%s]: " +
                "%s does not declare any static, non-private, non-final, nested classes " +
                "annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
      }
    }

    return ClassUtils.toClassArray(configClasses);
  }

  /**
   * Determine if the supplied {@link Class} meets the criteria for being
   * considered a <em>default configuration class</em> candidate.
   * <p>Specifically, such candidates:
   * <ul>
   * <li>must not be {@code null}</li>
   * <li>must not be {@code private}</li>
   * <li>must not be {@code final}</li>
   * <li>must be {@code static}</li>
   * <li>must be annotated or meta-annotated with {@code @Configuration}</li>
   * </ul>
   *
   * @param clazz the class to check
   * @return {@code true} if the supplied class meets the candidate criteria
   */
  private static boolean isDefaultConfigurationClassCandidate(@Nullable Class<?> clazz) {
    return (clazz != null && isStaticNonPrivateAndNonFinal(clazz) &&
            AnnotatedElementUtils.hasAnnotation(clazz, Configuration.class));
  }

  private static boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    int modifiers = clazz.getModifiers();
    return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
  }

}
