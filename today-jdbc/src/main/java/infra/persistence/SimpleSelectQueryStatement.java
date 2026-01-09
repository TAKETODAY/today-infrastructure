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

import java.util.ArrayList;
import java.util.Arrays;

import infra.persistence.platform.Platform;
import infra.persistence.sql.SimpleSelect;

/**
 * An abstract base class for constructing simple SQL SELECT query statements.
 * This class provides a framework for rendering a {@link SimpleSelect} statement
 * based on the provided {@link EntityMetadata}. Subclasses are responsible for
 * implementing the logic to customize the query, such as adding WHERE clauses
 * or other restrictions.
 *
 * <p>This class implements the {@link QueryStatement} interface and provides
 * a default implementation of the {@code render} method. The core logic for
 * customizing the query is delegated to the abstract method
 * {@link #renderInternal(EntityMetadata, SimpleSelect)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:19
 */
public abstract class SimpleSelectQueryStatement implements QueryStatement {

  /**
   * Renders a SQL SELECT statement sequence based on the provided entity metadata.
   *
   * <p>This method constructs a {@link SimpleSelect} object using the column names and table name
   * from the given {@link EntityMetadata}. It then delegates further rendering logic to the
   * abstract method {@link #renderInternal(EntityMetadata, SimpleSelect)} for customization.
   * Finally, the constructed {@link SimpleSelect} object is returned as a {@link StatementSequence}.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * EntityMetadata metadata = // obtain metadata for an entity
   * SimpleSelectQueryStatement renderer = new SimpleSelectQueryStatement() {
   *   @Override
   *   protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
   *     // Custom rendering logic, e.g., adding WHERE clauses
   *   }
   * };
   *
   * StatementSequence statement = renderer.render(metadata);
   * String sql = statement.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * <p>In the example above, the {@code renderInternal} method can be overridden to include
   * additional SQL clauses or conditions as needed.
   *
   * @param metadata the metadata of the entity for which the SQL SELECT statement is being rendered.
   * Must not be null. The metadata includes details such as table name, column names,
   * and entity properties.
   * @return a {@link StatementSequence} representing the rendered SQL SELECT statement. This can be
   * converted to a platform-specific SQL string using the
   * {@link StatementSequence#toStatementString(Platform)} method.
   */
  @Override
  public StatementSequence render(EntityMetadata metadata) {
    SimpleSelect select = new SimpleSelect(Arrays.asList(metadata.columnNames), new ArrayList<>());
    select.setTableName(metadata.tableName);

    renderInternal(metadata, select);
    return select;
  }

  /**
   * Provides the core logic for customizing a SQL SELECT statement based on the provided
   * entity metadata and an initialized {@link SimpleSelect} object.
   *
   * <p>This method is invoked by the {@link #render(EntityMetadata)} method after initializing
   * the {@link SimpleSelect} object with basic details such as table name and column names.
   * Subclasses should implement this method to add additional clauses or conditions to the
   * query, such as WHERE, ORDER BY, or LIMIT.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
   *   // Add a WHERE clause to filter results based on a specific condition
   *   select.addWhereClause("status = 'ACTIVE'");
   *
   *   // Add an ORDER BY clause to sort results
   *   select.addOrderByClause("created_at DESC");
   * }
   * }</pre>
   *
   * <p>In the example above, the {@code renderInternal} method is overridden to add a WHERE clause
   * and an ORDER BY clause to the SQL SELECT statement. This allows for flexible customization
   * of the query based on application-specific requirements.
   *
   * @param metadata the metadata of the entity for which the SQL SELECT statement is being rendered.
   * Must not be null. The metadata includes details such as table name, column names,
   * and entity properties.
   * @param select the initialized {@link SimpleSelect} object representing the SQL SELECT statement.
   * This object can be modified to include additional query elements like WHERE clauses.
   */
  protected abstract void renderInternal(EntityMetadata metadata, SimpleSelect select);

}
