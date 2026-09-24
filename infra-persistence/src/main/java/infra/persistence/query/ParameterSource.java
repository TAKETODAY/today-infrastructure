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

import infra.persistence.EntityMetadata;

/**
 * A source of bind parameters that can be applied to a JDBC
 * {@link PreparedStatement}.
 *
 * <p>A {@code ParameterSource} represents an object that holds the values to be
 * bound to the placeholder parameters ({@code ?}) of a prepared SQL statement
 * and knows how to apply them. JDBC parameter indexes start at {@code 1};
 * values must be bound in the same order as their placeholders in the rendered SQL.
 * The indexed binding method returns the next available index, allowing another
 * source or the caller to bind parameters immediately after this source.
 *
 * <p>This is the shared binding contract of {@link QueryStatement} and
 * {@link QueryCondition}, allowing callers that only need to bind parameters to
 * depend on it without pulling in SQL rendering or restriction collection.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryStatement
 * @see QueryCondition
 * @since 5.0
 */
public interface ParameterSource {

  /**
   * Bind this source's parameters starting at JDBC index {@code 1}.
   *
   * <p>Convenience form of {@link #setParameter(EntityMetadata, PreparedStatement, int)}
   * for statements whose first placeholder belongs to this source.
   *
   * @param metadata the metadata of the entity being queried
   * @param statement the statement to bind parameters to
   * @return the next available parameter index after the parameters bound by this source
   * @throws SQLException if a database access error occurs or a parameter index is invalid
   */
  default int setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    return setParameter(metadata, statement, 1);
  }

  /**
   * Bind this source's parameters beginning at the specified JDBC parameter index.
   * Bind values in the order of the corresponding placeholders in the rendered SQL,
   * consuming exactly the placeholders produced by this source.
   *
   * <p>Return the first unused index so that subsequent parameters can be bound
   * without inspecting JDBC parameter metadata. For example, binding two values
   * starting at index {@code 3} returns {@code 5}; binding no values returns
   * {@code 3}.
   *
   * @param metadata the metadata of the entity being queried
   * @param statement the statement to bind parameters to
   * @param parameterIndex the first index to bind, starting at {@code 1}
   * @return the next available parameter index after the parameters bound by this source
   * @throws SQLException if a database access error occurs or a parameter index is invalid
   */
  int setParameter(EntityMetadata metadata, PreparedStatement statement, int parameterIndex)
          throws SQLException;

}
