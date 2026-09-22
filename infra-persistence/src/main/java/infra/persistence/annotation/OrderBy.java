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
import infra.persistence.Order;

/**
 * Marks a mapped entity property as a sort key for example queries, using the
 * direction declared by {@link #value() value}.
 *
 * <p>Place on a mapped field or getter. Properties carrying the annotation take
 * part in the ORDER BY clause resolved from the entity metadata. They are ordered
 * by {@link #order() order}, ascending, so a lower value is applied first; keys
 * that share the same {@code order} keep their property declaration order (the
 * sort is stable). The entity's ID property may be ordered like any other
 * property.
 *
 * <p>Example:
 * <pre>{@code
 * @Table("t_user")
 * class User {
 *
 *   @Id
 *   Integer id;
 *
 *   @OrderBy // name ASC
 *   String name;
 *
 *   @OrderBy(value = Order.DESC, order = -1)     // applied before name
 *   LocalDateTime createdAt;
 * }
 * // ORDER BY created_at DESC, name ASC
 * }</pre>
 *
 * <p>The ordering of an example query is resolved with the following precedence,
 * where the earliest applicable source wins:
 * <ol>
 *   <li>an {@link infra.persistence.sql.OrderSpecSource} implemented by the example object;</li>
 *   <li>a class-level {@link OrderByClause @OrderByClause};</li>
 *   <li>property-level {@code @OrderBy} (this annotation).</li>
 * </ol>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OrderByClause
 * @see infra.persistence.sql.OrderSpecSource
 * @since 4.0 2024/3/31 17:21
 */
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderBy {

  /**
   * The sort direction of the annotated property.
   */
  Order value() default Order.ASC;

  /**
   * The precedence of this sort key among all {@link OrderBy @OrderBy} properties;
   * lower values are applied earlier. Defaults to {@code 0}.
   */
  int order() default 0;
}
