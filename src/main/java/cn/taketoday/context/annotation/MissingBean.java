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
 * @author TODAY 2019-01-31 14:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MissingBean {
  String MissingBeanMetadata = MissingBean.class.getName() + "-Metadata";

  /**
   * Missing bean name alias
   */
  @AliasFor("name")
  String value() default Constant.BLANK;

  /**
   * Missing bean name
   * <p>
   * this attr determine the bean definition
   * </p>
   *
   * <p>
   * when its declare on a method default bean name is method-name
   * </p>
   */
  @AliasFor("value")
  String name() default Constant.BLANK;

  /**
   * this attr determine the bean definition
   */
  Class<?> type() default void.class;

  /**
   * equals {@link #type()} ?
   *
   * @since 3.0
   */
  boolean equals() default false;

}
