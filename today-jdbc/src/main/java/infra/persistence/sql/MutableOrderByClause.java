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

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

import infra.core.Pair;
import infra.persistence.Order;

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
