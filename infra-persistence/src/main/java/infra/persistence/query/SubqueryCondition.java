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

package infra.persistence.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.persistence.EntityProperty;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;

/**
 * A {@link Condition} that renders a {@code targetColumn IN (SELECT ...)}
 * or {@code targetColumn NOT IN (SELECT ...)} predicate for many-to-many
 * lookups through a junction table.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class SubqueryCondition implements Condition {

  private final Identifier targetColumn;

  private final boolean negative;

  private final String select;

  private final Identifier tableName;

  private final Identifier sourceColumn;

  private final Object value;

  private final EntityProperty entityProperty;

  SubqueryCondition(Identifier targetColumn, boolean negative, String select, Identifier tableName,
          Identifier sourceColumn, Object value, EntityProperty entityProperty) {
    this.targetColumn = targetColumn;
    this.negative = negative;
    this.select = select;
    this.tableName = tableName;
    this.sourceColumn = sourceColumn;
    this.value = value;
    this.entityProperty = entityProperty;
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    sqlBuffer.append(targetColumn.render(platform));
    if (negative) {
      sqlBuffer.append(" NOT IN (SELECT ");
    }
    else {
      sqlBuffer.append(" IN (SELECT ");
    }
    sqlBuffer.append(select)
            .append(" FROM ")
            .append(tableName.render(platform))
            .append(" WHERE ")
            .append(sourceColumn.render(platform))
            .append(" = ?)");
  }

  @Override
  public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    entityProperty.setParameter(ps, parameterIndex++, value);
    return parameterIndex;
  }

}