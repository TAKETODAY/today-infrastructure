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
import java.util.ArrayList;
import java.util.List;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.annotation.OrderBy;
import infra.persistence.platform.Platform;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;

/**
 * Builds the conditional (WHERE / ORDER BY) part of a dynamic SQL statement and
 * binds its parameters.
 *
 * <p>A {@code QueryCondition} describes the conditional part of a query through
 * three cooperating operations, which callers must use consistently:
 * <ul>
 *   <li>{@link #collectRestrictions(EntityMetadata, List)} collects the
 *       {@link Restriction restrictions} that make up the WHERE clause</li>
 *   <li>{@link #resolveOrderByClause(EntityMetadata)} resolves the ORDER BY clause</li>
 *   <li>{@link ParameterSource#setParameter(EntityMetadata, PreparedStatement)} binds
 *       the values of the collected restrictions, in the same order</li>
 * </ul>
 *
 * <p>Collecting restrictions is the core contract; rendering them to SQL is a
   * convenience layered on top. {@link #appendWhereClause(Platform, EntityMetadata, StringBuilder)}
 * appends {@code " WHERE "} followed by the rendered restrictions when there is at
 * least one, while {@link #collectRestrictions(EntityMetadata)} returns the collected
 * list as-is.
 *
 * <p>Instances are typically created by a {@link EntityQueryFactory} (see
 * {@link EntityQueryFactories#createCondition}) and consumed by
 * {@link DefaultEntityManager} for {@code count}, {@code page} and {@code delete}
 * operations. Implementations should therefore be stateless, or at least safe for a
 * single collect-then-bind cycle under concurrent use.
 *
 * <p>Example:
 * <pre>{@code
 * QueryCondition condition = entityQueryFactory
 *         .createCondition(example);
 *
 * StringBuilder sql = new StringBuilder("SELECT * FROM t_user");
   * condition.appendWhereClause(platform, metadata, sql);
 *
 * try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
 *   condition.setParameter(metadata, statement);
 *   statement.executeQuery();
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryStatement
 * @see EntityQueryFactory
 * @see ParameterSource
 * @see Restriction
 * @see OrderByClause
 * @since 4.0 2024/3/31 15:51
 */
public interface QueryCondition extends ParameterSource {

  /**
   * Append the WHERE clause for the given entity to the supplied buffer, prefixed
   * with {@code " WHERE "} when at least one restriction applies.
   *
   * <p>This is a convenience combining {@link #collectRestrictions(EntityMetadata, List)}
   * with {@link Restriction#append(Platform, java.util.Collection, StringBuilder)}. When the
   * entity declares no condition, the buffer is left unchanged.
   *
   * @param platform the database platform whose rendering rules apply
   * @param metadata the metadata of the entity being queried
   * @param sql the buffer to append the rendered WHERE clause to
   * @since 5.0
   */
  default void appendWhereClause(Platform platform, EntityMetadata metadata, StringBuilder sql) {
    Restriction.append(platform, collectRestrictions(metadata), sql);
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
  default @Nullable OrderByClause resolveOrderByClause(EntityMetadata metadata) {
    MergedAnnotation<OrderBy> orderBy = metadata.getAnnotation(OrderBy.class);
    if (orderBy.isPresent()) {
      String clause = orderBy.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(clause)) {
        return OrderByClause.plain(clause);
      }
    }
    return null;
  }

}
