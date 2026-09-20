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
 * Declares how the {@link Group group} a property belongs to is joined to the
 * preceding top-level condition or group.
 *
 * <p>Place it on any member of a group; it sets the connector of that group as
 * a whole when it is placed among sibling conditions. Absent, the group joins
 * with {@code AND}:
 *
 * <pre>{@code
 * @Where("status > ?")
 * int status;
 *
 * @GroupConnector(LogicalOperator.OR)
 * @Group("state")
 * @Where(...)
 * int status2;
 *
 * @Group("state")
 * @Where(...)
 * int status3;
 * // status > ? OR (status2 = ? AND status3 <= ?)
 * }</pre>
 *
 * <p>This only affects the connection from the group to its predecessor.
 * Whether members connect with {@code AND} or {@link OR @OR} inside the group
 * is decided by the members themselves, exactly like top-level conditions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Group
 * @see OR
 * @since 5.0
 */
@Reflective
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupConnector {

  /**
   * The operator joining the group to the preceding top-level condition or
   * group.
   *
   * @return the connector, default {@link LogicalOperator#OR}
   */
  LogicalOperator value() default LogicalOperator.OR;

}