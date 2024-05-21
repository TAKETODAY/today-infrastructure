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

package cn.taketoday.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;

/**
 * Binds the value(s) of a URI matrix parameter to a resource method parameter,
 * resource class field, or resource class bean property. Values are URL decoded
 * A default value can be specified using the {@link #defaultValue()} attribute.
 * <p>
 * Note that the {@code @MatrixVariable} {@link #value() annotation value} refers
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
@Documented
@RequestParam
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MatrixVariable {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the matrix variable.
   *
   * @see #value
   * @since 4.0
   */
  @AliasFor("value")
  String name() default "";

  /**
   * The name of the URI path variable where the matrix variable is located,
   * if necessary for disambiguation (e.g. a matrix variable with the same
   * name present in more than one path segment).
   */
  String pathVar() default Constant.DEFAULT_NONE;

  /**
   * Whether the matrix variable is required.
   * <p>Default is {@code true}, leading to an exception being thrown in
   * case the variable is missing in the request. Switch this to {@code false}
   * if you prefer a {@code null} if the variable is missing.
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly sets
   * this flag to {@code false}.
   */
  boolean required() default true;

  /**
   * The default value to use as a fallback.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  String defaultValue() default Constant.DEFAULT_NONE;

}
