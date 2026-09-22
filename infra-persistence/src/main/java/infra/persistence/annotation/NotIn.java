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
import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Turns the annotated example property into a {@code column NOT IN (?, ?, ...)}
 * predicate. The property must hold a non-empty {@link Iterable} or array whose
 * elements are bound as the {@code NOT IN} list.
 *
 * <p>{@link #value()} aliases the {@link Column @Column} column name, so the
 * target column can be set without a separate {@code @Column}. When blank, the
 * property's mapped column (via {@code @Column} or the property name) is used:
 *
 * <pre>{@code
 * @NotIn("status")
 * List<Integer> excluded = List.of(1, 2);
 * // WHERE status NOT IN (?, ?)
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Column
 * @see In
 * @see Between
 * @since 5.0
 */
@Column
@Reflective
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotIn {

  /**
   * The column that the {@code NOT IN} operand targets.
   *
   * <p>An alias for {@link Column#value()}. When blank, the property's mapped
   * column is used instead.
   *
   * @return the column name, or {@link Constant#BLANK} if not specified
   */
  @AliasFor(annotation = Column.class, attribute = "value")
  String value() default Constant.BLANK;

}