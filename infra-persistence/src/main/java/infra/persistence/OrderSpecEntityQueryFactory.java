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
import java.util.List;

import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;

/**
 * Creates queries with ordering but no filtering from an {@link OrderSpec}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class OrderSpecEntityQueryFactory implements EntityQueryFactory {

  @Override
  public @Nullable QueryStatement createQuery(Object example) {
    return example instanceof OrderSpec spec ? new OrderSpecQuery(spec) : null;
  }

  @Override
  public @Nullable QueryCondition createCondition(Object example) {
    return example instanceof OrderSpec spec ? new OrderSpecQuery(spec) : null;
  }

  private static final class OrderSpecQuery extends SimpleSelectQueryStatement implements QueryCondition {

    private final OrderSpec spec;

    OrderSpecQuery(OrderSpec spec) {
      this.spec = spec;
    }

    @Override
    protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
      select.orderBy(spec);
    }

    @Override
    public OrderSpec resolveOrderByClause(EntityMetadata metadata) {
      return spec;
    }

    @Override
    public void collectRestrictions(EntityMetadata metadata, List<Restriction> restrictions) {
      // An order specification does not filter rows.
    }

    @Override
    public void setParameter(EntityMetadata metadata, PreparedStatement statement) {
      // No parameters to bind.
    }
  }

}
