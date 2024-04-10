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

package cn.taketoday.jdbc.persistence;

import java.util.List;

import javax.annotation.Nullable;

import cn.taketoday.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
final class QueryHandlerFactories implements QueryHandlerFactory {

  final List<QueryHandlerFactory> factories;

  QueryHandlerFactories(EntityMetadataFactory entityMetadataFactory) {
    List<QueryHandlerFactory> list = TodayStrategies.find(QueryHandlerFactory.class);
    list.add(new MapQueryHandlerFactory());
    list.add(new DefaultQueryHandlerFactory(entityMetadataFactory));
    this.factories = List.copyOf(list);
  }

  @Nullable
  @Override
  public QueryStatement createQuery(Object example) {
    for (QueryHandlerFactory factory : factories) {
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
    for (QueryHandlerFactory factory : factories) {
      ConditionStatement condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

}
