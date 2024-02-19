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

import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/19 19:56
 */
class ExampleQuery extends AbstractColumnsQueryHandler {

  final Object example;

  final EntityMetadata exampleMetadata;
  final ArrayList<Condition> conditions = new ArrayList<>();

  ExampleQuery(Object example, EntityMetadata exampleMetadata) {
    this.example = example;
    this.exampleMetadata = exampleMetadata;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    boolean first = true;
    StringBuilder where = new StringBuilder();

    for (EntityProperty entityProperty : exampleMetadata.entityProperties) {
      Object propertyValue = entityProperty.getValue(example);
      if (propertyValue != null) {
        if (first) {
          first = false;
        }
        else {
          where.append(" AND ");
        }

        where.append('`')
                .append(entityProperty.columnName)
                .append('`')
                .append(" = ?");

        // and
        conditions.add(new Condition(entityProperty.typeHandler, propertyValue));
      }
    }

    if (!conditions.isEmpty()) {
      select.setWhereClause(where);
    }
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (Condition condition : conditions) {
      condition.typeHandler.setParameter(statement, idx++, condition.propertyValue);
    }
  }

  @Override
  public String getDescription() {
    return "Query entities with example";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Lookup entity using example: {}", example);
  }

  static class Condition {
    final Object propertyValue;
    final TypeHandler<Object> typeHandler;

    private Condition(TypeHandler<Object> typeHandler, Object propertyValue) {
      this.typeHandler = typeHandler;
      this.propertyValue = propertyValue;
    }
  }
}
