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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to specify the default value when binding to an immutable
 * property. This annotation can also be used with nested properties to indicate that a
 * value should always be bound (rather than binding {@code null}). The value from this
 * annotation will only be used if the property is not found in the property sources used
 * by the {@link Binder}. For example, if the property is present in the
 * {@link cn.taketoday.core.env.Environment} when binding to
 * {@link cn.taketoday.boot.context.properties.ConfigurationProperties @ConfigurationProperties},
 * the default value for the property will not be used even if the property value is
 * empty.
 *
 * @author Madhura Bhave
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface DefaultValue {

  /**
   * The default value of the property. Can be an array of values for collection or
   * array-based properties.
   *
   * @return the default value of the property.
   */
  String[] value() default {};

}
