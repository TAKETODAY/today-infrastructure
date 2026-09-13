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

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.annotation.OrderBy;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;

/**
 * Builds the conditional (WHERE / ORDER BY) part of a dynamic SQL statement and
 * binds its parameters.
 *
 * <p>A {@code ConditionStatement} offloads two cooperating pieces of work to its
 * implementations, which callers must use consistently:
 * <ul>
 *   <li>{@link #collectRestrictions(EntityMetadata, List)} collects the
 *       {@link Restriction restrictions} that describe the WHERE clause</li>
 *   <li>{@link #setParameter(EntityMetadata, PreparedStatement)} binds their values
 *       in the same order the restrictions were rendered</li>
 * </ul>
 *
 * <p>Rendering to SQL is deliberately kept out of the core contract. The convenience
 * {@link #appendWhereClause(EntityMetadata, StringBuilder)} appends {@code " WHERE "}
 * followed by the rendered restrictions when there is at least one, and
 * {@link #collectRestrictions(EntityMetadata)} returns the collected list as-is.
 *
 * <p>Instances are typically created by a {@link QueryStatementFactory} (see
 * {@link QueryStatementFactories#createCondition}) and consumed by
 * {@link DefaultEntityManager} for {@code count}, {@code page} and {@code delete}
 * operations. Implementations should therefore be stateless, or at least safe for a
 * single render-then-bind cycle under concurrent use.
 *
 * <p>Example:
 * <pre>{@code
 * ConditionStatement condition = queryStatementFactory
 *         .createCondition(example);
 *
 * StringBuilder sql = new StringBuilder("SELECT * FROM t_user");
 * condition.renderWhereClause(metadata, sql);
 *
 * try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
 *   condition.setParameter(metadata, statement);
 *   statement.executeQuery();
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryStatement
 * @see QueryStatementFactory
 * @see Restriction
 * @see OrderByClause
 * @since 4.0 2024/3/31 15:51
 */
public interface ConditionStatement {

  /**
   * Render the WHERE clause for the given entity into the supplied buffer, prefixed
   * with {@code " WHERE "} when at least one restriction applies.
   *
   * <p>This is a convenience combining {@link #collectRestrictions(EntityMetadata, List)}
   * with {@link Restriction#append(java.util.Collection, StringBuilder)}. When the
   * entity declares no condition, the buffer is left unchanged.
   *
   * @param metadata the metadata of the entity being queried
   * @param sql the buffer to append the rendered WHERE clause to
   * @since 5.0
   */
  default void appendWhereClause(EntityMetadata metadata, StringBuilder sql) {
    Restriction.append(collectRestrictions(metadata), sql);
  }

  /**
   * Collect the WHERE clause restrictions for the given entity without rendering them.
   *
   * @param metadata the metadata of the entity being queried
   * @return a new mutable list of restrictions; empty when the entity declares no condition
   * @see #collectRestrictions(EntityMetadata, List)
   * @since 5.0
   */
  default List<Restriction> collectRestrictions(EntityMetadata metadata) {
    ArrayList<Restriction> restrictions = new ArrayList<>();
    collectRestrictions(metadata, restrictions);
    return restrictions;
  }

  /**
   * Add the {@link Restriction restrictions} forming the WHERE clause for the given
   * entity to the supplied list.
   *
   * <p>Implementations must not render SQL here; rendering is the caller's
   * responsibility. When no condition applies, the list must be left unchanged, since
   * an empty restriction list produces no WHERE clause at all.
   *
   * @param metadata the metadata of the entity being queried
   * @param restrictions the list to append the restrictions to
   */
  void collectRestrictions(EntityMetadata metadata, List<Restriction> restrictions);

  /**
   * Resolve the ORDER BY clause for the given entity, typically from an
   * {@link OrderBy @OrderBy} annotation declared on the entity class or its properties.
   *
   * @param metadata the metadata of the entity being queried
   * @return the resolved {@link OrderByClause}, or {@code null} when the entity
   * declares no ordering
   * @see OrderBy
   */
  default @Nullable OrderByClause getOrderByClause(EntityMetadata metadata) {
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
   * Bind the values backing the rendered restrictions to the given statement.
   *
   * <p>Parameter indexes start at {@code 1} and must follow the same order in which
   * the restrictions were rendered by
   * {@link #collectRestrictions(EntityMetadata, List)}. Each implementation is expected
   * to consume exactly the placeholders it produced.
   *
   * @param metadata the metadata of the entity being queried
   * @param statement the statement to bind parameters to
   * @throws SQLException if a database access error occurs or a parameter index is invalid
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement)
          throws SQLException;

}
