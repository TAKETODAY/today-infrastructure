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

import java.util.List;
import java.util.Map;

import cn.taketoday.core.Pair;
import cn.taketoday.lang.Assert;
import cn.taketoday.persistence.Order;
import cn.taketoday.util.StringUtils;

/**
 * OrderBy Clause
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public interface OrderByClause {

  CharSequence toClause();

  boolean isEmpty();

  // Static Factory Methods

  static MutableOrderByClause forMap(Map<String, Order> sortKeys) {
    MutableOrderByClause clause = new MutableOrderByClause(sortKeys.size());
    for (Map.Entry<String, Order> entry : sortKeys.entrySet()) {
      clause.orderBy(entry.getKey(), entry.getValue());
    }
    return clause;
  }

  @SafeVarargs
  static MutableOrderByClause valueOf(Pair<String, Order>... sortKeys) {
    Assert.notNull(sortKeys, "sortKeys is required");
    return new MutableOrderByClause(List.of(sortKeys));
  }

  static OrderByClause plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  static MutableOrderByClause mutable() {
    return new MutableOrderByClause();
  }

  /**
   * Plain
   */
  class Plain implements OrderByClause {

    final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public CharSequence toClause() {
      return sequence;
    }

    @Override
    public boolean isEmpty() {
      return StringUtils.isBlank(sequence);
    }

  }

}
