/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.gradle.junit;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.gradle.testkit.GradleBuild;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against multiple
 * versions of Gradle. Test classes using the extension must have a non-private and
 * non-final {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(GradleCompatibilityExtension.class)
public @interface GradleCompatibility {

  /**
   * Whether to include running Gradle with {@code --cache-configuration} cache in the
   * compatibility matrix.
   *
   * @return {@code true} to enable the configuration cache, {@code false} otherwise
   */
  boolean configurationCache() default false;

  String versionsLessThan() default "";

}
