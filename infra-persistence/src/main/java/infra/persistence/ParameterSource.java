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

package infra.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A source of bind parameters that can be applied to a JDBC
 * {@link PreparedStatement}.
 *
 * <p>A {@code ParameterSource} represents an object that holds the values to be
 * bound to the placeholder parameters ({@code ?}) of a prepared SQL statement
 * and knows how to apply them. Parameter indexes start at {@code 1} and must
 * follow the same order in which the corresponding placeholders appear in the
 * rendered SQL.
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
   * Apply the parameters held by this source to the given statement.
   *
   * <p>Parameter indexes start at {@code 1} and must follow the same order in
   * which the corresponding placeholders appeared when the SQL was rendered.
   * Each implementation is expected to consume exactly the placeholders it
   * produced.
   *
   * @param metadata the metadata of the entity being queried
   * @param statement the statement to bind parameters to
   * @throws SQLException if a database access error occurs or a parameter index is invalid
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement)
          throws SQLException;

}
