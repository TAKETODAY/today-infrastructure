/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

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
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default "";

  /**
   * The name of the matrix variable.
   *
   * @see #value
   * @since 4.0
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
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
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

  /**
   * The default value to use as a fallback.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "defaultValue")
  String defaultValue() default Constant.DEFAULT_NONE;

}
