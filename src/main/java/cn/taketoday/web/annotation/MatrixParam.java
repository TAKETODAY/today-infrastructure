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

package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.Constant;

/**
 * Binds the value(s) of a URI matrix parameter to a resource method parameter,
 * resource class field, or resource class bean property. Values are URL decoded
 * A default value can be specified using the {@link #defaultValue()} attribute.
 * <p>
 * Note that the {@code @MatrixParam} {@link #value() annotation value} refers
 * to a name of a matrix parameter that resides in the last matched path segment
 * of the {@link PathVariable}-annotated Java structure that injects the value
 * of the matrix parameter.
 * </p>
 * <p>
 * The type {@code T} of the annotated parameter, field or property must either:
 * </p>
 * <ol>
 * <li>Be a primitive type</li>
 * <li>Have a constructor that accepts a single {@code String} argument</li>
 * <li>Have a static method named {@code valueOf} or {@code fromString} that
 * accepts a single {@code String} argument (see, for example,
 * {@link Integer#valueOf(String)})</li>
 * <li>Be {@code List<T>}, {@code Set<T>} or {@code SortedSet<T>}, where
 * {@code T} satisfies 2, 3 or 4 above. The resulting collection is read-only.
 * </li>
 * </ol>
 *
 * <p>
 * If the type is not one of the collection types listed in 5 above and the
 * matrix parameter is represented by multiple values then the first value
 * (lexically) of the parameter is used.
 * </p>
 *
 * <p>
 * Because injection occurs at object creation time, use of this annotation
 * on resource class fields and bean properties is only supported for the
 * default per-request resource class lifecycle. Resource classes using
 * other lifecycles should only use this annotation on resource method parameters.
 * </p>
 *
 * @author TODAY 2021/10/4 11:47
 * @see <a href="http://www.w3.org/DesignIssues/MatrixURIs.html">Matrix URIs</a>
 * @since 4.0
 */
@RequestParam
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MatrixParam {

  /** Request parameter name in path */
  String value() default Constant.BLANK;

  /**
   * If required == true when request parameter is null, will be throws exception
   */
  boolean required() default false;

  /** When required == false, and parameter == null. use default value. */
  String defaultValue() default Constant.BLANK;

}
