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

import cn.taketoday.lang.Constant;

/**
 * @author TODAY 2018-08-04 13:13
 * @see cn.taketoday.context.annotation.PropsReader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER })
public @interface Props {

  /**
   * Properties file name
   *
   * @return Properties file name
   */
  String[] value() default {};

  /**
   * Property prefix
   * <p>
   * The prefix defaults to "". The framework will only connect
   * this prefix to the property name. Don't forget the'.' or
   * the binding will fail if the corresponding property is not found.
   */
  String[] prefix() default Constant.BLANK;

  /** Replace prefix. */
  boolean replace() default false;

  /**
   * Parent Declarative Setting Nested Class
   * <p>
   * Generally used at the outer level, there is no declaration
   * and if the Props annotation is not used inside the class,
   * it is ignored (stop recursion or iteration)
   */
  Class<?>[] nested() default {};

}
