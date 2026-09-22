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

package infra.persistence.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.persistence.sql.Restriction;

/**
 * The bindable layer on top of a {@link Restriction}: a SQL predicate that also
 * knows its bindable value(s).
 *
 * <p>A {@link Restriction} is a valueless SQL fragment. A {@code Condition}
 * pairs such a fragment with a bindable value, so the same object drives both
 * rendering and JDBC parameter binding.
 *
 * <p>How conditions are joined together ({@code AND}/{@code OR}/{@code XOR}) is
 * assembly-time metadata and deliberately stays out of this interface: the code
 * that puts conditions into a {@code WHERE} clause owns the connectors. A
 * condition only knows itself.
 *
 * <p>Because a condition owns its value, binding must be applied in the same
 * order in which the conditions are rendered: the index returned by
 * {@link #setParameter} advances past the placeholder(s) this condition
 * produced. The default implementation consumes no parameter, which is correct
 * for conditions that render no placeholder (for example {@code IS NULL});
 * conditions that bind a value must override it.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restriction
 * @see PropertyCondition
 * @see ConditionGroup
 * @since 5.0
 */
public interface Condition extends Restriction {

  /**
   * Bind this condition's value(s) at the given JDBC parameter index.
   *
   * @param ps the prepared statement to bind
   * @param parameterIndex the one-based parameter index
   * @return the next parameter index
   * @throws SQLException if the value cannot be bound
   */
  default int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    return parameterIndex;
  }

}