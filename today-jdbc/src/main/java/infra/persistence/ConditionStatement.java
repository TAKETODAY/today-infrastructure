/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.lang.Descriptive;
import infra.lang.Nullable;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;

/**
 * Represents a conditional statement used in query construction.
 * This interface provides methods for rendering WHERE clauses, applying
 * order-by clauses, and setting parameters for prepared statements.
 *
 * <p>Implementations of this interface are typically used to dynamically
 * generate SQL queries based on entity metadata and restrictions.</p>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p>1. Implementing a query with a map of parameters:</p>
 * <pre>{@code
 * static class MapQueryStatement extends SimpleSelectQueryStatement
 *         implements QueryStatement, ConditionStatement, DebugDescriptive {
 *
 *     private final Map<?, ?> map;
 *
 *     public MapQueryStatement(Map<?, ?> map) {
 *       this.map = map;
 *     }
 *
 *     @Override
 *     protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
 *       renderWhereClause(metadata, select.restrictions);
 *     }
 *
 *     @Override
 *     public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
 *       for (Map.Entry<?, ?> entry : map.entrySet()) {
 *         restrictions.add(Restriction.equal(entry.getKey().toString()));
 *       }
 *     }
 *
 *     @Override
 *     public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *       int idx = 1;
 *       for (Map.Entry<?, ?> entry : map.entrySet()) {
 *         statement.setObject(idx++, entry.getValue());
 *       }
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *       return "Query with Map of params: " + map;
 *     }
 * }
 * }</pre>
 *
 * <p>2. Implementing a query without conditions:</p>
 * <pre>{@code
 * final class NoConditionsQuery extends ColumnsQueryStatement implements ConditionStatement, DebugDescriptive {
 *
 *   static final NoConditionsQuery instance = new NoConditionsQuery();
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, Select select) {
 *     // noop
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *     // noop
 *   }
 *
 *   @Override
 *   public String getDescription() {
 *     return "Query entities without conditions";
 *   }
 *
 *   @Override
 *   public Object getDebugLogMessage() {
 *     return LogMessage.format(getDescription());
 *   }
 *
 *   @Override
 *   public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
 *     // noop
 *   }
 * }
 * }</pre>
 *
 * <p>3. Extending functionality for complex queries:</p>
 * <pre>{@code
 * class ExampleQuery extends SimpleSelectQueryStatement implements ConditionStatement {
 *
 *     private final Object example;
 *     private final EntityMetadata exampleMetadata;
 *     private final List<ConditionPropertyExtractor> extractors;
 *
 *     public ExampleQuery(Object example, EntityMetadata exampleMetadata, List<ConditionPropertyExtractor> extractors) {
 *       this.example = example;
 *       this.exampleMetadata = exampleMetadata;
 *       this.extractors = extractors;
 *     }
 *
 *     @Override
 *     public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
 *       // Add custom restrictions based on example object
 *     }
 *
 *     @Override
 *     public OrderByClause getOrderByClause(EntityMetadata metadata) {
 *       // Resolve and return order-by clause
 *       return super.getOrderByClause(metadata);
 *     }
 *
 *     @Override
 *     public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *       // Set parameters for the prepared statement
 *     }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Pageable
 * @see Descriptive
 * @see DebugDescriptive
 * @since 4.0 2024/3/31 15:51
 */
public interface ConditionStatement {

  /**
   * Renders the WHERE clause of a SQL query based on the provided entity metadata and restrictions.
   *
   * <p>This method generates the WHERE clause by analyzing the given {@link EntityMetadata} and
   * applying the specified {@link Restriction}s. The resulting WHERE clause is typically used
   * in dynamic query generation for filtering database records.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain metadata for the entity
   * List<Restriction> restrictions = Arrays.asList(
   *     new Restriction("age", ">", 30),
   *     new Restriction("status", "=", "active")
   * );
   *
   * ConditionStatement statement = new Condition();
   * statement.renderWhereClause(metadata, restrictions);
   * }</pre>
   *
   * <p>In this example, the method processes the metadata and restrictions to generate a WHERE
   * clause such as:
   * <pre>{@code
   * WHERE age > 30 AND status = 'active'
   * }</pre>
   *
   * @param metadata the metadata of the entity, providing information about its structure
   * and mapping to the database table
   * @param restrictions a list of restrictions defining the conditions to be applied in the
   * WHERE clause; each restriction specifies a column, operator, and value
   */
  void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions);

  /**
   * Retrieves the ORDER BY clause for a given entity based on its metadata.
   *
   * <p>This method checks if the provided {@link EntityMetadata} contains an {@link OrderBy}
   * annotation. If the annotation is present and its value is not equal to {@code DEFAULT_NONE},
   * it creates and returns an {@link OrderByClause} using the annotation's value. Otherwise,
   * it returns {@code null}.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain metadata for the entity
   * ConditionStatement statement = new Condition();
   *
   * OrderByClause orderByClause = statement.getOrderByClause(metadata);
   * if (orderByClause != null) {
   *   System.out.println("ORDER BY: " + orderByClause.toClause());
   * } else {
   *   System.out.println("No ORDER BY clause defined.");
   * }
   * }</pre>
   *
   * <p>In this example, if the metadata contains an {@code @OrderBy("name ASC")} annotation,
   * the output will be:
   * <pre>{@code
   * ORDER BY: name ASC
   * }</pre>
   *
   * @param metadata the metadata of the entity, which may contain an {@link OrderBy} annotation
   * @return an {@link OrderByClause} representing the ORDER BY clause, or {@code null} if
   * no valid ORDER BY clause is defined in the metadata
   */
  @Nullable
  default OrderByClause getOrderByClause(EntityMetadata metadata) {
    MergedAnnotation<OrderBy> orderBy = metadata.getAnnotation(OrderBy.class);
    if (orderBy.isPresent()) {
      String clause = orderBy.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(clause)) {
        return OrderByClause.plain(clause);
      }
    }
    return null;
  }

  /**
   * Sets parameters in a {@link PreparedStatement} based on the provided {@link EntityMetadata}.
   *
   * <p>This method is responsible for binding values from the {@link EntityMetadata} to the
   * corresponding placeholders in the {@link PreparedStatement}. It ensures that all required
   * parameters are correctly set before executing the SQL query.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain metadata for the entity
   * String sql = "SELECT * FROM users WHERE age > ? AND status = ?";
   * try (PreparedStatement statement = connection.prepareStatement(sql)) {
   *   ConditionStatement condition = new Condition();
   *   condition.setParameter(metadata, statement);
   *   ResultSet resultSet = statement.executeQuery();
   *   while (resultSet.next()) {
   *     // Process the result set
   *   }
   * } catch (SQLException e) {
   *   e.printStackTrace();
   * }
   * }</pre>
   *
   * <p>In this example, the method binds the values from the metadata to the placeholders in the
   * prepared statement, allowing the query to execute with the correct parameters.
   *
   * @param metadata the metadata of the entity, providing information about its structure and
   * mapping to the database table
   * @param statement the {@link PreparedStatement} to which the parameters will be bound
   * @throws SQLException if a database access error occurs or the parameter index is out of range
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement)
          throws SQLException;

}
