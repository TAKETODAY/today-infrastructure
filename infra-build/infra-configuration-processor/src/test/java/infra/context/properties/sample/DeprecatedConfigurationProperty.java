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

package infra.context.properties.sample;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a getter in a {@link ConfigurationProperties @ConfigurationProperties}
 * object is deprecated. This annotation has no bearing on the actual binding processes,
 * but it is used by the {@code spring-boot-configuration-processor} to add deprecation
 * meta-data.
 * <p>
 * This annotation <strong>must</strong> be used on the getter of the deprecated element.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeprecatedConfigurationProperty {

  /**
   * The reason for the deprecation.
   *
   * @return the deprecation reason
   */
  String reason() default "";

  /**
   * The field that should be used instead (if any).
   *
   * @return the replacement field
   */
  String replacement() default "";

  /**
   * The version in which the property became deprecated.
   *
   * @return the version
   */
  String since() default "";

}
