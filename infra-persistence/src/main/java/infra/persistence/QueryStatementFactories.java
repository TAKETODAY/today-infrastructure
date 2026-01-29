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
