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

package infra.context.properties.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.properties.ConfigurationProperties;
import infra.core.env.Environment;

/**
 * Annotation that can be used to specify the default value when binding to an immutable
 * property. This annotation can also be used with nested properties to indicate that a
 * value should always be bound (rather than binding {@code null}). The value from this
 * annotation will only be used if the property is not found in the property sources used
 * by the {@link Binder}. For example, if the property is present in the
 * {@link Environment} when binding to
 * {@link ConfigurationProperties @ConfigurationProperties},
 * the default value for the property will not be used even if the property value is
 * empty.
 *
 * @author Madhura Bhave
 * @author Pavel Anisimov
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
public @interface DefaultValue {

  /**
   * The default value of the property. Can be an array of values for collection or
   * array-based properties.
   *
   * @return the default value of the property.
   */
  String[] value() default {};

}
