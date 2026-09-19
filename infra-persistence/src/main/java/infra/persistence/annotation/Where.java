/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.lang.Constant;

/**
 * Defines a where-clause predicate for the annotated element, such as filtering
 * query results or applying runtime constraints. It is typically applied to
 * fields of an entity used as an example in a query.
 *
 * <p>The predicate is derived from the annotation attributes as follows:
 * <ul>
 *   <li>{@link #value()} — a complete SQL predicate fragment, e.g.
 *   {@code "status > ?"}, rendered unchanged;</li>
 *   <li>{@link #operator()} — a comparison operator applied to the property's
 *   column, e.g. {@code ">"}, rendering {@code column > ?};</li>
 *   <li>neither — the property's column is compared for equality, rendering
 *   {@code column = ?}.</li>
 * </ul>
 *
 * <p>Usage on a field:
 * <pre>{@code
 * @Where(value = "status > ?")
 * private int status;
 * }</pre>
 *
 * @author Emmanuel Bernard
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Reflective
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Where {

  /**
   * A complete where-clause predicate fragment to apply.
   *
   * <p>When set to {@link Constant#DEFAULT_NONE}, the predicate falls back to
   * {@link #operator()} or, failing that, to an equality comparison.
   *
   * @return the predicate fragment, or {@link Constant#DEFAULT_NONE} if not specified
   */
  String value() default Constant.DEFAULT_NONE;

  /**
   * A comparison operator applied to the property's column.
   *
   * <p>Only consulted when {@link #value()} is not specified. When both are
   * {@link Constant#DEFAULT_NONE}, an equality comparison is used.
   *
   * @return the comparison operator, or {@link Constant#DEFAULT_NONE} if not specified
   */
  String operator() default Constant.DEFAULT_NONE;

}