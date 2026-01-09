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

import infra.lang.Descriptive;

/**
 * Represents a query statement that can be rendered into SQL and
 * applied to a JDBC {@link PreparedStatement}. This interface provides
 * methods for generating SQL statements and setting parameters for
 * execution.
 *
 * <p>Implementations of this interface are typically used to encapsulate
 * the logic for constructing SQL queries dynamically based on entity metadata
 * and application-specific conditions.
 *
 * <p><b>Usage Examples:</b>
 *
 * <p>1. A query statement that uses a map of parameters:
 * <pre>{@code
 * static class MapQueryStatement extends SimpleSelectQueryStatement
 *         implements QueryStatement, ConditionStatement, DebugDescriptive {
 *
 *   private final Map<?, ?> map;
 *
 *   public MapQueryStatement(Map<?, ?> map) {
 *     this.map = map;
 *   }
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
 *     renderWhereClause(metadata, select.restrictions);
 *   }
 *
 *   @Override
 *   public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
 *     for (Map.Entry<?, ?> entry : map.entrySet()) {
 *       restrictions.add(Restriction.equal(entry.getKey().toString()));
 *     }
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *     int idx = 1;
 *     for (Map.Entry<?, ?> entry : map.entrySet()) {
 *       statement.setObject(idx++, entry.getValue());
 *     }
 *   }
 *
 *   @Override
 *   public String getDescription() {
 *     return "Query with Map of params: " + map;
 *   }
 * }
 * }</pre>
 *
 * <p>2. A query statement that fetches an entity by its ID:
 * <pre>{@code
 * class FindByIdQuery extends ColumnsQueryStatement implements QueryStatement, DebugDescriptive {
 *   private final Object id;
 *
 *   FindByIdQuery(Object id) {
 *     this.id = id;
 *   }
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, Select select) {
 *     select.setWhereClause('`' + metadata.idColumnName + "`=? LIMIT 1");
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *     metadata.idProperty().setParameter(statement, 1, id);
 *   }
 *
 *   @Override
 *   public String getDescription() {
 *     return "Fetch entity By ID";
 *   }
 * }
 * }</pre>
 *
 * <p>3. A query statement that applies an order-by clause:
 * <pre>{@code
 * class NoConditionsOrderByQuery extends ColumnsQueryStatement implements QueryStatement {
 *   private final OrderByClause clause;
 *
 *   NoConditionsOrderByQuery(OrderByClause clause) {
 *     this.clause = clause;
 *   }
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, Select select) {
 *     if (!clause.isEmpty()) {
 *       select.setOrderByClause(clause.toClause());
 *     }
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException { }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DebugDescriptive
 * @see Descriptive
 * @since 4.0 2024/2/16 14:45
 */
public interface QueryStatement {

  /**
   * Renders a sequence of SQL statements based on the provided entity metadata.
   *
   * <p>This method generates a {@link StatementSequence} object that encapsulates
   * the SQL statements required for operations involving the given entity. The
   * generated statements are typically used for database interactions such as
   * inserts, updates, or queries.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain entity metadata
   * QueryStatement queryStatement = new Query();
   * StatementSequence statementSequence = queryStatement.render(metadata);
   *
   * // Convert the statement sequence to an SQL string for a specific platform
   * String sql = statementSequence.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * @param metadata the metadata of the entity for which the SQL statements are generated;
   * must not be null. This includes details such as table name, column mappings,
   * and properties of the entity.
   * @return a {@link StatementSequence} object representing the rendered SQL statements.
   * The returned object can be further processed to generate platform-specific SQL strings.
   */
  StatementSequence render(EntityMetadata metadata);

  /**
   * Sets parameters in the provided {@code PreparedStatement} based on the given {@code EntityMetadata}.
   *
   * <p>This method is responsible for mapping the entity's properties to the corresponding
   * parameters in the prepared statement. It uses the metadata to determine the appropriate
   * values and their positions in the statement. This is typically used in database operations
   * such as inserts or updates.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain entity metadata
   * PreparedStatement statement = connection.prepareStatement("INSERT INTO table (col1, col2) VALUES (?, ?)");
   *
   * QueryStatement queryStatement = new Query();
   * queryStatement.setParameter(metadata, statement);
   *
   * statement.executeUpdate();
   * }</pre>
   *
   * @param metadata the metadata of the entity containing details such as column mappings
   * and property information; must not be null
   * @param statement the prepared statement where parameters will be set; must not be null
   * @throws SQLException if a database access error occurs while setting the parameters
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException;

}
