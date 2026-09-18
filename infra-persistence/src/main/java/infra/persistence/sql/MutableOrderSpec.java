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

import infra.core.Pair;
import infra.persistence.Identifier;
import infra.persistence.Order;
import infra.persistence.platform.Platform;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 19:51
 */
public class MutableOrderSpec implements OrderSpec {

  private final ArrayList<Pair<Identifier, Order>> sortKeys;

  public MutableOrderSpec() {
    this.sortKeys = new ArrayList<>();
  }

  public MutableOrderSpec asc(String col) {
    return asc(Identifier.parse(col));
  }

  public MutableOrderSpec asc(Identifier col) {
    sortKeys.add(Pair.of(col, Order.ASC));
    return this;
  }

  public MutableOrderSpec desc(String col) {
    return desc(Identifier.parse(col));
  }

  public MutableOrderSpec desc(Identifier col) {
    sortKeys.add(Pair.of(col, Order.DESC));
    return this;
  }

  public MutableOrderSpec orderBy(String col, Order order) {
    return orderBy(Identifier.parse(col), order);
  }

  public MutableOrderSpec orderBy(Identifier col, Order order) {
    return orderBy(Pair.of(col, order));
  }

  public MutableOrderSpec orderBy(Pair<Identifier, Order> sortKey) {
    sortKeys.add(sortKey);
    return this;
  }

  public MutableOrderSpec merge(@Nullable MutableOrderSpec orderByClause) {
    if (orderByClause != null) {
      sortKeys.addAll(orderByClause.sortKeys);
    }
    return this;
  }

  public boolean isEmpty() {
    return sortKeys.isEmpty();
  }

  @Override
  public CharSequence toClause(Platform platform) {
    StringBuilder orderByClause = new StringBuilder();
    boolean first = true;
    for (var entry : sortKeys) {
      if (first) {
        first = false;
        orderByClause.append(entry.first.render(platform))
                .append(' ').append(entry.second.name());
      }
      else {
        orderByClause.append(", ")
                .append(entry.first.render(platform))
                .append(' ').append(entry.second.name());
      }
    }
    return orderByClause;
  }

}
