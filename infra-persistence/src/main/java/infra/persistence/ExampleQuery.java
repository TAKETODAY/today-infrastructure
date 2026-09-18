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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import infra.core.annotation.MergedAnnotation;
import infra.logging.LogMessage;
import infra.persistence.PropertyConditionStrategy.Condition;
import infra.persistence.annotation.OR;
import infra.persistence.annotation.OrderBy;
import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.OrderSpecSource;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;

/**
 * A query statement that builds SQL conditions based on a non-null example object.
 * <p>
 * This class scans the properties of the provided example instance. For each non-null property,
 * it applies configured {@link PropertyConditionStrategy} instances to generate corresponding
 * {@link Restriction} objects for the WHERE clause. It also resolves the ORDER BY clause for
 * the query.
 *
 * <p>The ORDER BY clause is resolved in the following priority order:
 * <ol>
 *   <li>{@link OrderSpecSource} — a programmatic spec contributed by the example object itself;</li>
 *   <li>class-level {@link infra.persistence.annotation.OrderByClause @OrderByClause} — a raw SQL fragment;</li>
 *   <li>property-level {@link infra.persistence.annotation.OrderBy @OrderBy} — ordered by each key's
 *   {@link infra.persistence.annotation.OrderBy#order() precedence}.</li>
 * </ol>
 * An earlier source wins over later ones. The ID property is scanned like any other
 * property and may therefore be ordered as well.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OrderSpecSource
 * @see infra.persistence.annotation.OrderBy
 * @see infra.persistence.annotation.OrderByClause
 * @since 4.0 2024/2/19 19:56
 */
final class ExampleQuery extends SimpleSelectQueryStatement implements QueryCondition, DebugDescriptive {

  private final Object example;

  private final EntityMetadata exampleMetadata;

  private final List<PropertyConditionStrategy> strategies;

  private @Nullable ArrayList<Condition> conditions;

  ExampleQuery(Object example, EntityMetadata exampleMetadata, List<PropertyConditionStrategy> strategies) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
    this.strategies = strategies;
  }

  ExampleQuery(EntityMetadataFactory factory, Object example, List<PropertyConditionStrategy> strategies) {
    this.example = example;
    this.strategies = strategies;
    this.exampleMetadata = factory.getEntityMetadata(example.getClass());
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
    scan(select::addRestriction);
    select.orderBy(resolveOrderByClause(metadata));
  }

  @Override
  public void collectRestrictions(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(scan(null));
  }

  @Override
  public @Nullable OrderSpec resolveOrderByClause(EntityMetadata metadata) {
    // 1. programmatic source takes precedence
    if (example instanceof OrderSpecSource source) {
      OrderSpec spec = source.orderSpec();
      if (!spec.isEmpty()) {
        return spec;
      }
    }
    // 2. class-level raw clause, from the example's own metadata
    OrderSpec classLevel = QueryCondition.super.resolveOrderByClause(exampleMetadata);
    if (classLevel != null) {
      return classLevel;
    }
    // 3. property-level keys, in property declaration order
    return resolvePropertyOrderBy();
  }

  /**
   * Collect the {@link OrderBy @OrderBy} keys of the example's properties into an
   * {@link OrderSpec}, ordered by each key's {@link OrderBy#order() precedence}.
   *
   * <p>This is independent of the WHERE-condition scan, so ordering resolution
   * stays reliable regardless of when it is invoked.
   *
   * @return the property ordering spec, or {@code null} when no property declares one
   */
  private @Nullable OrderSpec resolvePropertyOrderBy() {
    ArrayList<SortKey> sortKeys = null;
    for (EntityProperty property : exampleMetadata.getEntityProperties(false)) {
      MergedAnnotation<OrderBy> annotation = property.getAnnotation(OrderBy.class);
      if (annotation.isPresent()) {
        if (sortKeys == null) {
          sortKeys = new ArrayList<>();
        }
        sortKeys.add(new SortKey(
                annotation.getInt("order"),
                property.getColumnName(),
                annotation.getEnum("value", Order.class)));
      }
    }
    if (sortKeys == null) {
      return null;
    }
    sortKeys.sort(Comparator.comparingInt(SortKey::order));
    OrderSpec.Builder builder = OrderSpec.builder();
    for (SortKey sortKey : sortKeys) {
      builder.orderBy(sortKey.column, sortKey.direction);
    }
    return builder.build();
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
      EntityProperty[] entityProperties = exampleMetadata.getEntityProperties(false);
      conditions = new ArrayList<>(entityProperties.length);

      for (EntityProperty property : entityProperties) {
        Object propertyValue = property.getValue(example);
        if (propertyValue != null) {
          boolean logicalAnd = !property.isPresent(OR.class);

          for (var strategy : strategies) {
            var condition = strategy.resolve(logicalAnd, property, propertyValue);
            if (condition != null) {
              if (consumer != null) {
                consumer.accept(condition);
              }
              conditions.add(condition);
              break;
            }
          }
        } // todo 构建 null 的情况
      }
      this.conditions = conditions;
    }
    else if (consumer != null) {
      conditions.forEach(consumer);
    }
    return conditions;
  }

  private record SortKey(int order, Identifier column, Order direction) {
  }
}
