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
 * Turns the annotated example property into a {@code column IN (?, ?, ...)}
 * predicate. The property must hold a non-empty {@link Iterable} or array whose
 * elements are bound as the {@code IN} list.
 *
 * <pre>{@code
 * @In
 * List<Integer> statuses = List.of(1, 2, 3);
 * // WHERE status IN (?, ?, ?)
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Between
 * @since 5.0
 */
@Reflective
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface In {

  /**
   * The column that the {@code IN} operand targets.
   *
   * <p>When set to {@link Constant#DEFAULT_NONE}, the property's mapped column is
   * used instead.
   *
   * @return the column name, or {@link Constant#DEFAULT_NONE} if not specified
   */
  String column() default Constant.DEFAULT_NONE;

}