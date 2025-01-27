/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.persistence;

import java.util.List;

import infra.lang.Nullable;
import infra.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
final class QueryStatementFactories implements QueryStatementFactory {

  final List<QueryStatementFactory> factories;

  QueryStatementFactories(EntityMetadataFactory entityMetadataFactory, List<ConditionPropertyExtractor> extractors) {
    List<QueryStatementFactory> list = TodayStrategies.find(QueryStatementFactory.class);
    list.add(new MapQueryStatementFactory());
    list.add(new DefaultQueryStatementFactory(entityMetadataFactory, extractors));
    this.factories = List.copyOf(list);
  }

  @Nullable
  @Override
  public QueryStatement createQuery(Object example) {
    for (QueryStatementFactory factory : factories) {
      QueryStatement query = factory.createQuery(example);
      if (query != null) {
        return query;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public ConditionStatement createCondition(Object example) {
    for (QueryStatementFactory factory : factories) {
      ConditionStatement condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

}
