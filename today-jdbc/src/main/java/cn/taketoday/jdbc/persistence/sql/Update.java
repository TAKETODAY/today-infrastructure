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

package cn.taketoday.jdbc.persistence.sql;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import cn.taketoday.jdbc.persistence.StatementSequence;
import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.lang.Nullable;

/**
 * A SQL {@code UPDATE} statement.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("UnusedReturnValue")
public class Update implements StatementSequence {

  protected String tableName;

  @Nullable
  protected CharSequence comment;

  protected final ArrayList<Restriction> restrictions = new ArrayList<>();

  protected final LinkedHashMap<String, String> assignments = new LinkedHashMap<>();

  public String getTableName() {
    return tableName;
  }

  public Update setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public Update setComment(@Nullable CharSequence comment) {
    this.comment = comment;
    return this;
  }

  public Update addAssignments(String... columnNames) {
    for (String columnName : columnNames) {
      addAssignment(columnName);
    }
    return this;
  }

  public Update addAssignment(String columnName) {
    return addAssignment(columnName, "?");
  }

  public Update addAssignment(String columnName, String valueExpression) {
    assignments.put(columnName, valueExpression);
    return this;
  }

  public Update addRestriction(String column) {
    restrictions.add(new ComparisonRestriction(column));
    return this;
  }

  public Update addRestriction(String... columns) {
    for (final String columnName : columns) {
      if (columnName != null) {
        addRestriction(columnName);
      }
    }
    return this;
  }

  public Update addRestriction(String column, String value) {
    restrictions.add(new ComparisonRestriction(column, value));
    return this;
  }

  public Update addRestriction(String column, ComparisonRestriction.Operator op, String value) {
    restrictions.add(new ComparisonRestriction(column, op, value));
    return this;
  }

  public Update addColumnIsNullRestriction(String columnName) {
    restrictions.add(new NullnessRestriction(columnName));
    return this;
  }

  public Update addColumnIsNotNullRestriction(String columnName) {
    restrictions.add(new NullnessRestriction(columnName, false));
    return this;
  }

  @Override
  public String toStatementString() {
    final var buf = new StringBuilder((assignments.size() * 15) + tableName.length() + 10);

    applyComment(buf);
    buf.append("UPDATE ").append(tableName);
    applyAssignments(buf);
    applyRestrictions(buf);

    return buf.toString();
  }

  private void applyComment(StringBuilder buf) {
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
  }

  private void applyAssignments(StringBuilder buf) {
    buf.append(" set ");
    final var entries = assignments.entrySet().iterator();
    while (entries.hasNext()) {
      final var entry = entries.next();
      buf.append('`').append(entry.getKey()).append('`')
              .append('=').append(entry.getValue());
      if (entries.hasNext()) {
        buf.append(", ");
      }
    }
  }

  private void applyRestrictions(StringBuilder buf) {
    Restriction.render(restrictions, buf);
  }

}
