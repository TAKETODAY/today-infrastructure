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
import infra.persistence.sql.LogicalOperator;

/**
 * Declares how the annotated property connects to the preceding term.
 *
 * <p>Connection is either <em>member</em> ({@link #group() group() false}) —
 * between a condition and the preceding top-level condition, or between the
 * members inside a group — or <em>group</em> ({@code group() true}) — between a
 * whole group and the preceding top-level condition or group. The default,
 * {@code AND}, is implied when no annotation is present.
 *
 * <p>The convenience annotations are shorthand for this one:
 * {@link OR @OR} equals {@code @Connector(LogicalOperator.OR)} and
 * {@link GroupOR @GroupOR} equals {@code @Connector(value = LogicalOperator.OR, group = true)}.
 *
 * <pre>{@code
 * @Where("status > ?")
 * int status;
 *
 * @Connector(value = LogicalOperator.OR)
 * @Where(operator = " = ")
 * int status2;                          // ... AND status2 = ?
 *
 * @Connector(value = LogicalOperator.OR, group = true)
 * @Group("state")
 * @Where(operator = " <= ")
 * int status3;                          // (status... AND status2) OR (...)
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LogicalOperator
 * @see OR
 * @see GroupOR
 * @see Group
 * @since 5.0
 */
@Reflective
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Connector {

  /**
   * The logical operator joining this property to the preceding term.
   *
   * @return the operator, default {@link LogicalOperator#AND}
   */
  LogicalOperator value() default LogicalOperator.AND;

  /**
   * Whether this connector applies to a whole {@link Group group} rather than a
   * single condition member.
   *
   * <p>When {@code true}, the property carries a {@link Group} and this operator
   * joins the group as a whole to the preceding top-level condition or group.
   * When {@code false} (the default), the operator joins this single condition
   * to its predecessor.
   *
   * @return {@code true} for an inter-group connector
   */
  boolean group() default false;

}