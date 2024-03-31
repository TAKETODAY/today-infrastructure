/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.Pair;
import cn.taketoday.jdbc.persistence.Order;
import cn.taketoday.lang.Assert;

/**
 * OrderBy Clause
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public class OrderByClause {

  private final ArrayList<Pair<String, Order>> sortKeys;

  public OrderByClause() {
    this.sortKeys = new ArrayList<>();
  }

  public OrderByClause(Collection<Pair<String, Order>> sortKeys) {
    this.sortKeys = new ArrayList<>(sortKeys);
  }

  OrderByClause(int initialCapacity) {
    this.sortKeys = new ArrayList<>(initialCapacity);
  }

  public OrderByClause asc(String col) {
    sortKeys.add(Pair.of(col, Order.ASC));
    return this;
  }

  public OrderByClause desc(String col) {
    sortKeys.add(Pair.of(col, Order.DESC));
    return this;
  }

  public OrderByClause orderBy(String col, Order order) {
    return orderBy(Pair.of(col, order));
  }

  public OrderByClause orderBy(Pair<String, Order> sortKey) {
    sortKeys.add(sortKey);
    return this;
  }

  public boolean isEmpty() {
    return sortKeys.isEmpty();
  }

  public CharSequence toClause() {
    StringBuilder orderByClause = new StringBuilder();
    boolean first = true;
    for (var entry : sortKeys) {
      if (first) {
        first = false;
        orderByClause.append('`');
        orderByClause.append(entry.first)
                .append("` ")
                .append(entry.second.name());
      }
      else {
        orderByClause.append(", `")
                .append(entry.first)
                .append("` ")
                .append(entry.second.name());
      }
    }
    return orderByClause;
  }

  public static OrderByClause forMap(Map<String, Order> sortKeys) {
    OrderByClause clause = new OrderByClause(sortKeys.size());
    for (Map.Entry<String, Order> entry : sortKeys.entrySet()) {
      clause.orderBy(entry.getKey(), entry.getValue());
    }
    return clause;
  }

  @SafeVarargs
  public static OrderByClause valueOf(Pair<String, Order>... sortKeys) {
    Assert.notNull(sortKeys, "sortKeys is required");
    return new OrderByClause(List.of(sortKeys));
  }

}
