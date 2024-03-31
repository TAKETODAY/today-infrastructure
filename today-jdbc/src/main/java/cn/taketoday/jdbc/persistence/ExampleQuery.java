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
import java.util.function.Consumer;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.jdbc.persistence.PropertyConditionStrategy.Condition;
import cn.taketoday.jdbc.persistence.sql.MutableOrderByClause;
import cn.taketoday.jdbc.persistence.sql.OrderByClause;
import cn.taketoday.jdbc.persistence.sql.OrderBySource;
import cn.taketoday.jdbc.persistence.sql.Restriction;
import cn.taketoday.jdbc.persistence.sql.SimpleSelect;
import cn.taketoday.jdbc.persistence.support.DefaultConditionStrategy;
import cn.taketoday.jdbc.persistence.support.FuzzyQueryConditionStrategy;
import cn.taketoday.jdbc.persistence.support.WhereAnnotationConditionStrategy;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/19 19:56
 */
final class ExampleQuery extends SimpleSelectQueryHandler implements ConditionHandler {

  static final List<PropertyConditionStrategy> strategies;

  static {
    List<PropertyConditionStrategy> list = TodayStrategies.find(PropertyConditionStrategy.class);
    list.add(new WhereAnnotationConditionStrategy());
    list.add(new FuzzyQueryConditionStrategy());
    list.add(new DefaultConditionStrategy());
    strategies = List.copyOf(list);
  }

  private final Object example;

  private final EntityMetadata exampleMetadata;

  @Nullable
  private ArrayList<Condition> conditions;

  @Nullable
  private OrderByClause orderByClause;

  ExampleQuery(EntityMetadataFactory factory, Object example) {
    this.example = example;
    this.exampleMetadata = factory.getEntityMetadata(example.getClass());
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    scan(condition -> select.addRestriction(condition.restriction));
    select.orderBy(example instanceof OrderBySource source ? source.getOrderByClause() : orderByClause);
  }

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(scan(null));
  }

  @Override
  public OrderByClause getOrderByClause(EntityMetadata metadata) {
    if (example instanceof OrderBySource source) {
      OrderByClause orderByClause = source.getOrderByClause();
      if (!orderByClause.isEmpty()) {
        return orderByClause;
      }
    }
    return orderByClause;
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (var condition : scan(null)) {
      idx = condition.setParameter(statement, idx);
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

  private ArrayList<Condition> scan(@Nullable Consumer<Condition> consumer) {
    ArrayList<Condition> conditions = this.conditions;
    if (conditions == null) {
      conditions = new ArrayList<>(exampleMetadata.entityProperties.length);
      // apply class level order by
      applyOrderByClause();

      for (EntityProperty entityProperty : exampleMetadata.entityProperties) {
        Object propertyValue = entityProperty.getValue(example);
        if (propertyValue != null) {
          for (var strategy : strategies) {
            var condition = strategy.resolve(entityProperty, propertyValue);
            if (condition != null) {
              if (consumer != null) {
                consumer.accept(condition);
              }
              conditions.add(condition);
            }
          }
        }

        applyOrderByClause(entityProperty);
      }
      this.conditions = conditions;
    }
    else if (consumer != null) {
      conditions.forEach(consumer);
    }
    return conditions;
  }

  private void applyOrderByClause() {
    MergedAnnotation<OrderBy> orderBy = exampleMetadata.getAnnotation(OrderBy.class);
    if (orderBy.isPresent()) {
      String clause = orderBy.getString("clause");
      if (!Constant.DEFAULT_NONE.equals(clause)) {
        orderByClause = OrderByClause.plain(clause);
      }
    }
  }

  private void applyOrderByClause(EntityProperty entityProperty) {
    if (!(orderByClause instanceof OrderByClause.Plain)) {
      MergedAnnotation<OrderBy> annotation = entityProperty.getAnnotation(OrderBy.class);
      if (annotation.isPresent()) {
        Order direction = annotation.getEnum("direction", Order.class);
        MutableOrderByClause mutable = (MutableOrderByClause) orderByClause;
        if (mutable == null) {
          mutable = OrderByClause.mutable();
          this.orderByClause = mutable;
        }
        mutable.orderBy(entityProperty.columnName, direction);
      }
    }
  }
}
