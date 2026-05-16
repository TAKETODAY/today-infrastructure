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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import infra.persistence.sql.MutableOrderByClause;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;
import infra.util.CollectionUtils;

/**
 * Chainable condition builder that implements both {@link ConditionStatement} and
 * {@link QueryStatement}, usable directly with {@link EntitySearch#where(ConditionStatement)}.
 *
 * <pre>{@code
 * em.search(User.class)
 *   .where(QueryCondition.of(q -> q
 *       .eq("status", "active")
 *       .gt("age", 18)
 *       .orderByDesc("created_at")))
 *   .list();
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@SuppressWarnings("NullAway")
public class QueryCondition extends SimpleSelectQueryStatement implements ConditionStatement {

  private final List<Restriction> restrictions = new ArrayList<>();
  private final List<Object> parameters = new ArrayList<>();
  private final MutableOrderByClause orderByClause = new MutableOrderByClause();

  // --- comparison operators ---

  public QueryCondition eq(String column, Object value) {
    restrictions.add(Restriction.equal(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition ne(String column, Object value) {
    restrictions.add(Restriction.notEqual(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition gt(String column, Object value) {
    restrictions.add(Restriction.graterThan(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition ge(String column, Object value) {
    restrictions.add(Restriction.graterEqual(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition lt(String column, Object value) {
    restrictions.add(Restriction.lessThan(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition le(String column, Object value) {
    restrictions.add(Restriction.lessEqual(column));
    parameters.add(value);
    return this;
  }

  public QueryCondition like(String column, String value) {
    restrictions.add(Restriction.forOperator(column, " LIKE ", "?"));
    parameters.add(value);
    return this;
  }

  public QueryCondition notLike(String column, String value) {
    restrictions.add(Restriction.forOperator(column, " NOT LIKE ", "?"));
    parameters.add(value);
    return this;
  }

  public QueryCondition between(String column, Object start, Object end) {
    restrictions.add(Restriction.between(column));
    parameters.add(start);
    parameters.add(end);
    return this;
  }

  public QueryCondition in(String column, Object... values) {
    restrictions.add(inRestriction(column, values.length));
    CollectionUtils.addAll(parameters, values);
    return this;
  }

  public QueryCondition in(String column, Collection<?> values) {
    restrictions.add(inRestriction(column, values.size()));
    parameters.addAll(values);
    return this;
  }

  public QueryCondition notIn(String column, Object... values) {
    restrictions.add(notInRestriction(column, values.length));
    CollectionUtils.addAll(parameters, values);
    return this;
  }

  public QueryCondition isNull(String column) {
    restrictions.add(Restriction.isNull(column));
    return this;
  }

  public QueryCondition isNotNull(String column) {
    restrictions.add(Restriction.isNotNull(column));
    return this;
  }

  // --- logical grouping ---

  /**
   * Group subsequent conditions with OR.
   * <pre>{@code
   * q.eq("status", "active")
   *  .or(() -> q.eq("role", "admin").eq("role", "moderator"));
   * }</pre>
   */
  public QueryCondition or(Runnable block) {
    int before = restrictions.size();
    block.run();
    wrapGroup(before, false);
    return this;
  }

  // --- ordering ---

  public QueryCondition orderByAsc(String column) {
    orderByClause.asc(column);
    return this;
  }

  public QueryCondition orderByDesc(String column) {
    orderByClause.desc(column);
    return this;
  }

  public QueryCondition orderBy(String column, Order order) {
    orderByClause.orderBy(column, order);
    return this;
  }

  public QueryCondition orderBy(Map<String, Order> sortKeys) {
    for (var entry : sortKeys.entrySet()) {
      orderByClause.orderBy(entry.getKey(), entry.getValue());
    }
    return this;
  }

  // --- QueryStatement implementation ---

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    for (Restriction restriction : restrictions) {
      select.addRestriction(restriction);
    }
    if (!orderByClause.isEmpty()) {
      select.orderBy(orderByClause);
    }
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int index = 1;
    for (Object param : parameters) {
      statement.setObject(index++, param);
    }
  }

  // --- ConditionStatement implementation ---

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(this.restrictions);
  }

  @Override
  public @Nullable OrderByClause getOrderByClause(EntityMetadata metadata) {
    return orderByClause.isEmpty() ? null : orderByClause;
  }

  // --- helpers ---

  private void wrapGroup(int before, boolean and) {
    int after = restrictions.size();
    if (after <= before) {
      return;
    }
    List<Restriction> group = new ArrayList<>(restrictions.subList(before, after));
    restrictions.subList(before, after).clear();
    Restriction combined = group.stream()
            .reduce((a, b) -> and ? Restriction.and(a, b) : Restriction.or(a, b))
            .orElseThrow();
    restrictions.add(combined);
  }

  private static Restriction inRestriction(String column, int count) {
    return Restriction.forOperator(column, " IN ", inPlaceholders(count));
  }

  private static Restriction notInRestriction(String column, int count) {
    return Restriction.forOperator(column, " NOT IN ", inPlaceholders(count));
  }

  private static String inPlaceholders(int count) {
    var sj = new StringJoiner(", ", "(", ")");
    for (int i = 0; i < count; i++) {
      sj.add("?");
    }
    return sj.toString();
  }

  /**
   * Create a {@code QueryCondition} configured by the given consumer.
   */
  public static QueryCondition of(java.util.function.Consumer<QueryCondition> configurer) {
    var condition = new QueryCondition();
    configurer.accept(condition);
    return condition;
  }

}
