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
import java.util.Collection;
import java.util.List;

import cn.taketoday.jdbc.persistence.PropertyConditionStrategy.Condition;
import cn.taketoday.jdbc.persistence.sql.Select;
import cn.taketoday.jdbc.persistence.support.DefaultConditionStrategy;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/19 19:56
 */
final class ExampleQuery extends AbstractColumnsQueryHandler {

  static final List<PropertyConditionStrategy> strategies = List.of(new DefaultConditionStrategy());

  private final Object example;

  private final EntityMetadata exampleMetadata;

  private final ArrayList<Condition> conditions = new ArrayList<>();

  ExampleQuery(Object example, EntityMetadata exampleMetadata) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    for (EntityProperty entityProperty : exampleMetadata.entityProperties) {
      Object propertyValue = entityProperty.getValue(example);
      if (propertyValue != null) {
        for (PropertyConditionStrategy strategy : strategies) {
          var condition = strategy.resolve(entityProperty, propertyValue);
          if (condition != null) {
            conditions.add(condition);
          }
        }
      }
    }

    StringBuilder where = new StringBuilder(conditions.size() * 12);
    renderWhereClause(conditions, where);
    select.setWhereClause(where);
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void renderWhereClause(Collection<Condition> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Condition condition : restrictions) {
      if (appended) {
        buf.append(" AND ");
      }
      else {
        appended = true;
      }
      condition.restriction.render(buf);
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
