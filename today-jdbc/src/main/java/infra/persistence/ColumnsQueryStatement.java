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

import infra.persistence.platform.Platform;
import infra.persistence.sql.Select;

/**
 * An abstract base class for constructing SQL SELECT statements
 * with column specifications. This class implements the {@link QueryStatement}
 * interface and provides a framework for rendering SQL queries dynamically
 * based on entity metadata.
 *
 * <p>This class is designed to be extended by concrete implementations that
 * provide additional query logic, such as WHERE clauses, ORDER BY clauses,
 * or other customizations. The core functionality of this class is to
 * generate the SELECT clause and FROM clause of an SQL statement based on
 * the provided {@link EntityMetadata}.
 *
 * <p>Subclasses must implement the {@link #renderInternal(EntityMetadata, Select)}
 * method to add custom query logic, such as appending WHERE or ORDER BY clauses.
 *
 * <h3>Usage Example</h3>
 *
 * Below is an example of how to extend this class to create a query that
 * fetches an entity by its ID:
 *
 * <pre>{@code
 * class FindByIdQuery extends ColumnsQueryStatement implements QueryStatement {
 *
 *   private final Object id;
 *
 *   FindByIdQuery(Object id) {
 *     this.id = id;
 *   }
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, Select select) {
 *     select.setWhereClause("`" + metadata.idColumnName + "`=? LIMIT 1");
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *     metadata.idProperty().setParameter(statement, 1, id);
 *   }
 * }
 * }</pre>
 *
 * In this example, the {@code FindByIdQuery} class extends {@code ColumnsQueryStatement}
 * and overrides the {@code renderInternal} method to append a WHERE clause for filtering
 * by the entity's ID. The {@code setParameter} method is used to bind the ID value to the
 * prepared statement.
 *
 * <h3>Rendering Logic</h3>
 *
 * The {@link #render(EntityMetadata)} method generates the SELECT clause by iterating
 * over the entity properties defined in the metadata. For each property, the corresponding
 * column name is appended to the SELECT clause. The FROM clause is set to the table name
 * specified in the metadata.
 *
 * <p>Example of the generated SQL for an entity with two properties:
 * <pre>
 * SELECT `column1`, `column2` FROM `table_name`
 * </pre>
 *
 * <h3>Customization</h3>
 *
 * Subclasses can customize the query further by overriding the {@code renderInternal}
 * method. For example, adding an ORDER BY clause:
 *
 * <pre>{@code
 * class NoConditionsOrderByQuery extends ColumnsQueryStatement {
 *
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
 * }
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 *
 * This class is not thread-safe. Instances should not be shared across threads
 * without proper synchronization.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryStatement
 * @see EntityMetadata
 * @see Select
 * @since 4.0 2024/2/16 18:10
 */
public abstract class ColumnsQueryStatement implements QueryStatement {

  /**
   * Renders a SQL SELECT statement sequence based on the provided entity metadata.
   *
   * <p>This method constructs a SELECT clause dynamically by iterating over the entity properties
   * defined in the metadata. It appends the column names of the properties to the SELECT clause,
   * ensuring proper formatting with backticks. The FROM clause is set to the table name specified
   * in the metadata. Additionally, the method delegates further rendering logic to the
   * {@link #renderInternal(EntityMetadata, Select)} method for customization.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain metadata for an entity
   * ColumnsQueryStatement queryStatement = new ColumnsQueryStatement() {
   *   @Override
   *   protected void renderInternal(EntityMetadata metadata, Select select) {
   *     // Custom rendering logic, if needed
   *   }
   * };
   * StatementSequence statementSequence = queryStatement.render(metadata);
   * String sql = statementSequence.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * <p>In the example above, the `render` method generates a SELECT statement sequence, which can
   * then be converted to a platform-specific SQL string using the `toStatementString` method.
   *
   * @param metadata the metadata of the entity for which the SELECT statement is being rendered.
   * Must not be null. Contains information about the table name, column mappings,
   * and entity properties.
   * @return a {@link StatementSequence} representing the rendered SQL SELECT statement. This object
   * can be further processed or converted to a string representation using the
   * {@link StatementSequence#toStatementString(Platform)} method.
   * @throws NullPointerException if the provided metadata is null or if any required property
   * within the metadata is missing.
   */
  @Override
  public StatementSequence render(EntityMetadata metadata) {
    Select select = new Select();
    StringBuilder selectClause = new StringBuilder();

    boolean first = true;
    for (EntityProperty property : metadata.entityProperties) {
      if (first) {
        first = false;
        selectClause.append('`');
      }
      else {
        selectClause.append(", `");
      }
      selectClause.append(property.columnName)
              .append('`');
    }

    select.setSelectClause(selectClause);
    select.setFromClause(metadata.tableName);

    renderInternal(metadata, select);
    return select;
  }

  /**
   * Renders additional internal components of a SQL SELECT statement based on the provided
   * entity metadata and the current state of the SELECT clause.
   *
   * <p>This method is invoked during the rendering process of a SQL SELECT statement. It allows
   * subclasses to customize or extend the rendering logic by adding specific clauses, conditions,
   * or modifications to the SELECT statement. The {@code metadata} parameter provides detailed
   * information about the entity, such as table name and column mappings, while the {@code select}
   * parameter represents the current state of the SELECT clause being constructed.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * EntityMetadata metadata = ...; // Obtain metadata for an entity
   * ColumnsQueryStatement queryStatement = new ColumnsQueryStatement() {
   *   @Override
   *   protected void renderInternal(EntityMetadata metadata, Select select) {
   *     // Add a custom WHERE clause
   *     select.where("age > ?", 18);
   *
   *     // Add an ORDER BY clause
   *     select.orderBy("`name` ASC");
   *   }
   * };
   * StatementSequence statementSequence = queryStatement.render(metadata);
   * String sql = statementSequence.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * <p>In the example above, the `renderInternal` method is overridden to add a WHERE clause and
   * an ORDER BY clause to the SELECT statement. This demonstrates how subclasses can leverage
   * this method to tailor the SQL generation process.
   *
   * @param metadata the metadata of the entity for which the SELECT statement is being rendered.
   * Must not be null. Contains information about the table name, column mappings,
   * and entity properties.
   * @param select the current state of the SELECT clause being constructed. This object allows
   * further modifications to the SQL statement, such as adding conditions, joins,
   * or ordering.
   */
  protected abstract void renderInternal(EntityMetadata metadata, Select select);

}
