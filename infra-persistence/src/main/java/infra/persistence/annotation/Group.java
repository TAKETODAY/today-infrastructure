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

/**
 * Groups several example properties into one parenthesized condition.
 *
 * <p>Properties carrying the same {@link #value() group name} are combined into
 * a single, parenthesized condition. The group takes the position of its first
 * member. This is how a non-linear {@code WHERE} clause is expressed
 * declaratively:
 *
 * <pre>{@code
 * @EntityRef(UserModel.class)
 * class UserQuery {
 *
 *   @Where("status > ?")
 *   int status;
 *
 *   @Group("state")
 *   @Where(operator = " = ")
 *   int status2;
 *
 *   @Group("state")
 *   @OR
 *   @Where(operator = " <= ")
 *   int status3;
 * }
 * // status > ? AND (status2 = ? OR status3 <= ?)
 * }</pre>
 *
 * <p>This annotation is purely structural: it decides which properties belong
 * together and how groups nest. Connection is declared separately — inside a
 * group members join the preceding member with {@code AND} unless they carry
 * {@link OR @OR}, and the group's link to the preceding term is declared with
 * {@link GroupConnector @GroupConnector}:
 *
 * <pre>{@code
 * @GroupConnector(LogicalOperator.OR)
 * @Group("g")
 * @Where(...)
 * int a;
 *
 * @Group("g")
 * @Where(...)
 * int b;
 * // (...) OR (a AND b)
 * }</pre>
 *
 * <p>A group name may be a dot-separated path, so groups nest to any depth:
 *
 * <pre>{@code
 * @Group("a") int x;   // level 1
 * @Group("a.b") int y;   // level 2, nested in "a"
 * @Group("a.b.c") int z;   // level 3, nested in "a.b"
 * // (x AND (y AND z))
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.ConditionGroup
 * @see GroupConnector
 * @see OR
 * @since 5.0
 */
@Reflective
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {

  /**
   * The group name. Properties annotated with the same name are combined into
   * one group.
   *
   * @return the group name
   */
  String value();

}