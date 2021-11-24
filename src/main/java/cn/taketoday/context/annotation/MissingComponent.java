/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;

/**
 * Context will create a bean definition when current context were missing
 *
 * @author TODAY 2021/7/15 21:36
 * @since 3.0.6
 */
@MissingBean
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MissingComponent {

  /**
   * Missing bean name
   * <p>
   * this attr determine the bean definition
   * </p>
   */
  @AliasFor(annotation = MissingBean.class)
  String value() default Constant.BLANK;

  /**
   * this attr determine the bean definition
   */
  @AliasFor(annotation = MissingBean.class)
  Class<?> type() default void.class;

  /**
   * equals {@link #type()} ?
   */
  @AliasFor(annotation = MissingBean.class)
  boolean equals() default false;

}
