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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.persistence.Order;

/**
 * Declares a keyset (seek) pagination sort key on an entity class.
 *
 * <p>Unlike {@link OrderBy @OrderBy}, which contributes to any ORDER BY clause,
 * this annotation describes the properties that form the stable keyset order used
 * by keyset pagination. Repeat the annotation on the entity class to declare a
 * composite order; keys are applied by {@link #order() precedence}, so a lower
 * value sorts first.
 *
 * <p>An annotated property drives the whole keyset page, not just the sort: it
 * contributes to the {@code ORDER BY} clause, to the cursor comparison in the
 * {@code WHERE} clause, and to the corresponding parameter binding. All keyset
 * properties taken together therefore also decide the shape of the
 * {@code nextCursor} map.
 *
 * <p>Example:
 * <pre>{@code
 * @Table("t_user")
 * @Keyset(property = "createdAt", direction = Order.DESC)
 * @Keyset(property = "name")
 * class User {
 *
 *   @Id
 *   Integer id;
 *
 *   LocalDateTime createdAt;
 *
 *   String name;
 * }
 * // keyset order: created_at DESC, name ASC, id ...
 * }</pre>
 *
 * <p>The entity ID is appended as a unique tie-breaker when it is not already
 * part of the declared order. The keyset order is only a default: a query-level
  * ordering supplied through
  * {@link infra.persistence.KeysetPageable#withOrder(infra.persistence.sql.OrderSpec)}
 * takes precedence over it.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see OrderBy
 * @see infra.persistence.KeysetPageable
 * @since 5.0
 */
@Reflective
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Keyset.List.class)
public @interface Keyset {

  /**
   * The name of the mapped entity property to sort by.
   */
  String property();

  /**
   * The sort direction of the property.
   */
  Order direction() default Order.ASC;

  /**
   * The precedence of this key among all {@link Keyset @Keyset} declarations;
   * lower values are applied earlier. Defaults to {@code 0}.
   */
  int order() default 0;

  /**
   * Container annotation for repeatable {@link Keyset @Keyset}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface List {

    Keyset[] value();
  }

}
