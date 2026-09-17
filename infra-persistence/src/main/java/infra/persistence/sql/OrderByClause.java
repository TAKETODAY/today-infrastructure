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

import java.util.Map;
import java.util.stream.Stream;

import infra.core.Pair;
import infra.persistence.Identifier;
import infra.persistence.Order;
import infra.persistence.platform.Platform;
import infra.util.Assert;
import infra.util.StringUtils;

/**
 * OrderBy Clause
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public interface OrderByClause {

  default CharSequence toClause() {
    return toClause(Platform.generic());
  }

  CharSequence toClause(Platform platform);

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
    return new MutableOrderByClause(Stream.of(sortKeys).map(pair -> Pair.of(Identifier.parse(pair.first), pair.second)).toList());
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
    public CharSequence toClause(Platform platform) {
      return sequence;
    }

    @Override
    public boolean isEmpty() {
      return StringUtils.isBlank(sequence);
    }

  }

}
