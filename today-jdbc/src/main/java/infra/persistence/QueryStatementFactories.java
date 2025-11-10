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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.lang.Assert;
import infra.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
@SuppressWarnings("rawtypes")
final class QueryStatementFactories implements QueryStatementFactory {

  final List<QueryStatementFactory> factories;

  QueryStatementFactories(EntityMetadataFactory metadataFactory, List<ConditionPropertyExtractor> extractors) {
    this(defaultFactories(metadataFactory, extractors));
  }

  QueryStatementFactories(List<QueryStatementFactory> factories) {
    this.factories = factories;
  }

  @Override
  public @Nullable QueryStatement createQuery(Object example) {
    Assert.notNull(example, "Example object is required");
    for (QueryStatementFactory factory : factories) {
      QueryStatement query = factory.createQuery(example);
      if (query != null) {
        return query;
      }
    }
    return null;
  }

  @Override
  public @Nullable ConditionStatement createCondition(Object example) {
    Assert.notNull(example, "Example object is required");
    for (QueryStatementFactory factory : factories) {
      ConditionStatement condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

  private static List<QueryStatementFactory> defaultFactories(EntityMetadataFactory entityMetadataFactory, List<ConditionPropertyExtractor> extractors) {
    List<QueryStatementFactory> list = TodayStrategies.find(QueryStatementFactory.class);
    list.add(new MapQueryStatementFactory());
    list.add(new DefaultQueryStatementFactory(entityMetadataFactory, extractors));
    return list;
  }

}
