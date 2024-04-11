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

package cn.taketoday.persistence.sql;

import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.core.Pair;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.Order;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 19:51
 */
public class MutableOrderByClause implements OrderByClause {

  private final ArrayList<Pair<String, Order>> sortKeys;

  public MutableOrderByClause() {
    this.sortKeys = new ArrayList<>();
  }

  public MutableOrderByClause(Collection<Pair<String, Order>> sortKeys) {
    this.sortKeys = new ArrayList<>(sortKeys);
  }

  MutableOrderByClause(int initialCapacity) {
    this.sortKeys = new ArrayList<>(initialCapacity);
  }

  public MutableOrderByClause asc(String col) {
    sortKeys.add(Pair.of(col, Order.ASC));
    return this;
  }

  public MutableOrderByClause desc(String col) {
    sortKeys.add(Pair.of(col, Order.DESC));
    return this;
  }

  public MutableOrderByClause orderBy(String col, Order order) {
    return orderBy(Pair.of(col, order));
  }

  public MutableOrderByClause orderBy(Pair<String, Order> sortKey) {
    sortKeys.add(sortKey);
    return this;
  }

  public MutableOrderByClause merge(@Nullable MutableOrderByClause orderByClause) {
    if (orderByClause != null) {
      sortKeys.addAll(orderByClause.sortKeys);
    }
    return this;
  }

  public boolean isEmpty() {
    return sortKeys.isEmpty();
  }

  @Override
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

}
