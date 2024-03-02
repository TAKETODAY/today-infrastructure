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

package cn.taketoday.jdbc.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;

/**
 * trim string property
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see String#trim()
 * @since 4.0 2024/2/24 22:45
 */
@Where
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimWhere {

  /**
   * The where-clause predicate.
   */
  @AliasFor(annotation = Where.class, attribute = "value")
  String value() default Constant.DEFAULT_NONE;

  @AliasFor(annotation = Where.class, attribute = "condition")
  String condition() default Constant.DEFAULT_NONE;

}
