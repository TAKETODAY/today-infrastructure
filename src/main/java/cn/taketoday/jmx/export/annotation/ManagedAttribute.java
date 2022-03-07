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

package cn.taketoday.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that indicates to expose a given bean property as a
 * JMX attribute, corresponding to the
 * {@link cn.taketoday.jmx.export.metadata.ManagedAttribute}.
 *
 * <p>Only valid when used on a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @see cn.taketoday.jmx.export.metadata.ManagedAttribute
 * @since 4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedAttribute {

  /**
   * Set the default value for the attribute in a {@link javax.management.Descriptor}.
   */
  String defaultValue() default "";

  /**
   * Set the description for the attribute in a {@link javax.management.Descriptor}.
   */
  String description() default "";

  /**
   * Set the currency time limit field in a {@link javax.management.Descriptor}.
   */
  int currencyTimeLimit() default -1;

  /**
   * Set the persistPolicy field in a {@link javax.management.Descriptor}.
   */
  String persistPolicy() default "";

  /**
   * Set the persistPeriod field in a {@link javax.management.Descriptor}.
   */
  int persistPeriod() default -1;

}
