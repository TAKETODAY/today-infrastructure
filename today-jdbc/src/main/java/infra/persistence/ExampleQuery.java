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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.logging.LogMessage;
import infra.persistence.PropertyConditionStrategy.Condition;
import infra.persistence.sql.MutableOrderByClause;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.OrderBySource;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;
import infra.persistence.support.DefaultConditionStrategy;
import infra.persistence.support.FuzzyQueryConditionStrategy;
import infra.persistence.support.WhereAnnotationConditionStrategy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/19 19:56
 */
@SuppressWarnings("rawtypes")
final class ExampleQuery extends SimpleSelectQueryStatement implements ConditionStatement, DebugDescriptive {

  static final List<PropertyConditionStrategy> strategies;

  static {
    List<PropertyConditionStrategy> list = TodayStrategies.find(PropertyConditionStrategy.class);
    list.add(new WhereAnnotationConditionStrategy());
    list.add(new FuzzyQueryConditionStrategy());
    list.add(new DefaultConditionStrategy());
    strategies = List.copyOf(list);
  }

  private final Object example;

  final EntityMetadata exampleMetadata;

  private final List<ConditionPropertyExtractor> extractors;

  @Nullable
  private ArrayList<Condition> conditions;

  @Nullable
  private OrderByClause orderByClause;

  ExampleQuery(Object example, EntityMetadata exampleMetadata, List<ConditionPropertyExtractor> extractors) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
    this.extractors = extractors;
  }

  ExampleQuery(EntityMetadataFactory factory, Object example, List<ConditionPropertyExtractor> extractors) {
    this.example = example;
    this.extractors = extractors;
    this.exampleMetadata = factory.getEntityMetadata(example.getClass());
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    scan(select::addRestriction);
    select.orderBy(example instanceof OrderBySource source ? source.orderByClause() : orderByClause);
  }

  public void renderWhereClause(StringBuilder sql) {
    Restriction.render(scan(null), sql);
  }

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(scan(null));
  }

  @Override
  public OrderByClause getOrderByClause(EntityMetadata metadata) {
    if (example instanceof OrderBySource source) {
      OrderByClause orderByClause = source.orderByClause();
      if (!orderByClause.isEmpty()) {
        return orderByClause;
      }
    }
    if (orderByClause == null) {
      orderByClause = ConditionStatement.super.getOrderByClause(metadata);
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

  @SuppressWarnings("unchecked")
  private ArrayList<Condition> scan(@Nullable Consumer<Condition> consumer) {
    ArrayList<Condition> conditions = this.conditions;
    if (conditions == null) {
      conditions = new ArrayList<>(exampleMetadata.entityProperties.length);
      // apply class level order by
      applyOrderByClause();

      for (EntityProperty property : exampleMetadata.entityProperties) {
        Object propertyValue = property.getValue(example);
        if (propertyValue != null) {
          Object extracted = propertyValue;
          ConditionPropertyExtractor selected = null;
          for (ConditionPropertyExtractor extractor : extractors) {
            Object extract = extractor.extract(example, property, propertyValue);
            if (extract != propertyValue) {
              selected = extractor;
              extracted = extract;
              break;
            }
          }

          if (extracted != null) {
            boolean logicalAnd = !property.isPresent(OR.class);

            for (var strategy : strategies) {
              var condition = strategy.resolve(logicalAnd, property, extracted);
              if (condition != null) {
                if (selected != null) {
                  if (Objects.equals(condition.value, extracted)) {
                    condition = condition.withValue(propertyValue);
                  }
                  else {
                    condition = condition.withValue(selected.wrap(extracted));
                  }
                }

                if (consumer != null) {
                  consumer.accept(condition);
                }
                conditions.add(condition);
                break;
              }
            }
          }
        }

        applyOrderByClause(property);
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
