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

import infra.persistence.platform.Platform;

/**
 * Renders the SQL of a query from an entity's {@link EntityMetadata} and binds
 * the values of the placeholders it produced.
 *
 * <p>A {@code QueryStatement} is the read-side counterpart of
 * {@link QueryCondition}: it builds a full statement — columns, table, WHERE,
 * ORDER BY and so on — rather than only the WHERE clause. It is used by
 * {@link EntityManager} for {@code find}, {@code iterate} and similar query
 * operations; insert and update statements are built directly from the entity
 * properties and do not go through this interface.
 *
 * <p>The contract is a two-phase, caller-driven cycle that must be executed in
 * the following order:
 * <ol>
 *   <li>{@link #render(EntityMetadata)} builds a dialect-neutral
 *       {@link StatementSequence}</li>
 *   <li>{@link StatementSequence#toStatementString(Platform)} turns it into a
 *       platform-specific SQL string</li>
 *   <li>{@link ParameterSource#setParameter(EntityMetadata, PreparedStatement)}
 *       binds the values, following the order of the {@code ?} placeholders
 *       emitted by {@code render}</li>
 * </ol>
 *
 * <p>The same {@link EntityMetadata} must be passed to both {@code render} and
 * {@code setParameter}, and neither the statement's state nor any example it
 * reads from may change in between, otherwise placeholders and bound values can
 * silently mismatch.
 *
 * <p>Rendering stays dialect-neutral until the sequence is turned into a SQL
 * string. Implementations must not hard-code a quoting character such as a
 * backtick: identifier quoting is resolved against the platform in
 * {@link StatementSequence#toStatementString(Platform)}, through
 * {@link Platform#toQuotedIdentifier(String)} and {@link Platform#quote(String)}.
 *
 * <p>Most implementations extend {@link SimpleSelectQueryStatement} and only
 * implement {@code renderInternal}; a {@link QueryCondition} can be reused here
 * as well. Implementations should be stateless, or at least safe for a single
 * render-then-bind cycle under concurrent use. Implementing
 * {@link DebugDescriptive} is optional but recommended, so that statements can
 * describe themselves in SQL logs and error messages.
 *
 * <p>Example:
 * <pre>{@code
 * class ActiveUsers extends SimpleSelectQueryStatement {
 *
 *   @Override
 *   protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
 *     select.addRestriction(Restriction.equal("status"));
 *   }
 *
 *   @Override
 *   public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
 *     statement.setString(1, "active");
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryCondition
 * @see ParameterSource
 * @see StatementSequence
 * @see DebugDescriptive
 * @since 4.0 2024/2/16 14:45
 */
public interface QueryStatement extends ParameterSource {

  /**
   * Render the SQL for the given entity into a dialect-neutral
   * {@link StatementSequence}.
   *
   * <p>The returned sequence is not yet a SQL string; pass it to
   * {@link StatementSequence#toStatementString(Platform)} to resolve the
   * platform dialect. This method must not emit dialect-specific text — in
   * particular it must not hard-code identifier quoting; resolve that against
   * the platform when the sequence is converted.
   *
   * <p>The {@code ?} placeholders emitted here fix the order in which
   * {@link ParameterSource#setParameter(EntityMetadata, PreparedStatement)}
   * must bind values.
   *
   * @param metadata the metadata of the entity to query; must not be {@code null}
   * @return the rendered statement sequence; never {@code null}
   * @see StatementSequence#toStatementString(Platform)
   */
  StatementSequence render(EntityMetadata metadata);

}
