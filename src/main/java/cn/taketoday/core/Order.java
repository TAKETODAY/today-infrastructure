/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Order} defines the sort order for an annotated component.
 *
 * <p>
 * The {@link #value} is optional and represents an order value as defined in
 * the {@link Ordered} interface. Higher values have higher priority. The
 * default value is {@code Ordered.LOWEST_PRECEDENCE}, indicating lowest
 * priority (losing to any other specified order value).
 *
 * @author TODAY<br>
 * 2018-11-07 13:15
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER })
public @interface Order {

  /**
   * The order value.
   * <p>
   * Default is {@link Ordered#LOWEST_PRECEDENCE}.
   *
   * @see Ordered#getOrder()
   */
  int value() default Ordered.LOWEST_PRECEDENCE;

}
