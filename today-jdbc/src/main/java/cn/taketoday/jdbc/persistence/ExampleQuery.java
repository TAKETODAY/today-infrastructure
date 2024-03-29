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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.jdbc.persistence.PropertyConditionStrategy.Condition;
import cn.taketoday.jdbc.persistence.sql.SimpleSelect;
import cn.taketoday.jdbc.persistence.support.DefaultConditionStrategy;
import cn.taketoday.jdbc.persistence.support.FuzzyQueryConditionStrategy;
import cn.taketoday.jdbc.persistence.support.WhereAnnotationConditionStrategy;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/19 19:56
 */
final class ExampleQuery extends SimpleSelectQueryHandler {

  static final List<PropertyConditionStrategy> strategies;

  static {
    List<PropertyConditionStrategy> strategyList = TodayStrategies.find(PropertyConditionStrategy.class);
    strategyList.add(new WhereAnnotationConditionStrategy());
    strategyList.add(new FuzzyQueryConditionStrategy());
    strategyList.add(new DefaultConditionStrategy());
    strategies = List.copyOf(strategyList);
  }

  private final Object example;

  private final EntityMetadata exampleMetadata;

  private final ArrayList<Condition> conditions = new ArrayList<>();

  ExampleQuery(Object example, EntityMetadata exampleMetadata) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    for (EntityProperty entityProperty : exampleMetadata.entityProperties) {
      Object propertyValue = entityProperty.getValue(example);
      if (propertyValue != null) {
        for (var strategy : strategies) {
          var condition = strategy.resolve(entityProperty, propertyValue);
          if (condition != null) {
            conditions.add(condition);
            select.addRestriction(condition.restriction);
          }
        }
      }
    }
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (var condition : conditions) {
      condition.entityProperty.setParameter(statement, idx++, condition.propertyValue);
    }
  }

  @Override
  public String getDescription() {
    return "Query entities with example";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Query entity using example: {}", example);
  }

}
