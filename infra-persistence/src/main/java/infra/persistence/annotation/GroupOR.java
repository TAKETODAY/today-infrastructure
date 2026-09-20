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
 * Shorthand for {@code @Connector(value = LogicalOperator.OR, group = true)}.
 *
 * <p>Declares that the whole {@link Group group} a property belongs to is joined
 * to the preceding top-level condition or group with {@code OR}.
 *
 * <p>This is the <em>inter-group</em> connector: it decides how the group is
 * connected to its predecessor, not how the members inside it connect to each
 * other. Members keep joining with {@code AND} unless they carry {@link OR @OR}.
 *
 * <pre>{@code
 * @Where("status > ?")
 * int status;
 *
 * @GroupOR
 * @Group("state")
 * @Where(operator = " = ")
 * int status2;
 *
 * @Group("state")
 * @Where(operator = " <= ")
 * int status3;
 * // status > ? OR (status2 = ? AND status3 <= ?)
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Connector
 * @see Group
 * @see OR
 * @since 5.0
 */
@Reflective
@Connector(group = true, value = LogicalOperator.OR)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupOR {

}