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

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * An SQL <tt>UPDATE</tt> statement
 * <p> from hibernate
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class Update {

  protected String where;
  protected String tableName;
  protected String assignments;
  protected String versionColumnName;

  protected String comment;

  protected final LinkedHashMap<String, String> columns = new LinkedHashMap<>();

  protected final LinkedHashMap<String, String> whereColumns = new LinkedHashMap<>();

  @Nullable
  protected LinkedHashMap<String, String> primaryKeyColumns;

  public String getTableName() {
    return tableName;
  }

  public Update appendAssignmentFragment(String fragment) {
    if (assignments == null) {
      assignments = fragment;
    }
    else {
      assignments += ", " + fragment;
    }
    return this;
  }

  public Update setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public Update setPrimaryKeyColumnNames(String... columnNames) {
    if (primaryKeyColumns == null) {
      primaryKeyColumns = new LinkedHashMap<>();
    }
    this.primaryKeyColumns.clear();
    addPrimaryKeyColumns(columnNames);
    return this;
  }

  public Update addPrimaryKeyColumns(String... columnNames) {
    for (String columnName : columnNames) {
      addPrimaryKeyColumn(columnName, "?");
    }
    return this;
  }

  public Update addPrimaryKeyColumns(String[] columnNames, boolean[] includeColumns, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      if (includeColumns[i]) {
        addPrimaryKeyColumn(columnNames[i], valueExpressions[i]);
      }
    }
    return this;
  }

  public Update addPrimaryKeyColumns(String[] columnNames, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      addPrimaryKeyColumn(columnNames[i], valueExpressions[i]);
    }
    return this;
  }

  public Update addPrimaryKeyColumn(String columnName, String valueExpression) {
    if (primaryKeyColumns == null) {
      primaryKeyColumns = new LinkedHashMap<>();
    }
    primaryKeyColumns.put(columnName, valueExpression);
    return this;
  }

  public Update setVersionColumnName(String versionColumnName) {
    this.versionColumnName = versionColumnName;
    return this;
  }

  public Update setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public Update addColumns(String... columnNames) {
    for (String columnName : columnNames) {
      addColumn(columnName);
    }
    return this;
  }

  public Update addColumns(String[] columnNames, boolean[] updateable, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      if (updateable[i]) {
        addColumn(columnNames[i], valueExpressions[i]);
      }
    }
    return this;
  }

  public Update addColumns(String[] columnNames, String valueExpression) {
    for (String columnName : columnNames) {
      addColumn(columnName, valueExpression);
    }
    return this;
  }

  public Update addColumn(String columnName) {
    return addColumn(columnName, "?");
  }

  public Update addColumn(String columnName, String valueExpression) {
    columns.put(columnName, valueExpression);
    return this;
  }

  public Update addWhereColumns(String... columnNames) {
    for (String columnName : columnNames) {
      addWhereColumn(columnName);
    }
    return this;
  }

  public Update addWhereColumns(String[] columnNames, String valueExpression) {
    for (String columnName : columnNames) {
      addWhereColumn(columnName, valueExpression);
    }
    return this;
  }

  public Update addWhereColumn(String columnName) {
    return addWhereColumn(columnName, "=?");
  }

  public Update addWhereColumn(String columnName, String valueExpression) {
    whereColumns.put(columnName, valueExpression);
    return this;
  }

  public Update setWhere(String where) {
    this.where = where;
    return this;
  }

  public String toStatementString() {

    StringBuilder buf = new StringBuilder((columns.size() * 15) + tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
    buf.append("update ").append(tableName).append(" set ");
    boolean assignmentsAppended = false;

    var iter = columns.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, String> e = iter.next();
      buf.append('`').append(e.getKey()).append('`')
              .append('=').append(e.getValue());
      if (iter.hasNext()) {
        buf.append(", ");
      }
      assignmentsAppended = true;
    }
    if (assignments != null) {
      if (assignmentsAppended) {
        buf.append(", ");
      }
      buf.append(assignments);
    }

    boolean conditionsAppended = false;
    if (CollectionUtils.isNotEmpty(primaryKeyColumns)
            || where != null
            || !whereColumns.isEmpty()
            || versionColumnName != null) {
      buf.append(" where ");
    }

    if (primaryKeyColumns != null) {
      iter = primaryKeyColumns.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, String> e = iter.next();
        buf.append(e.getKey()).append('=').append(e.getValue());
        if (iter.hasNext()) {
          buf.append(" and ");
        }
        conditionsAppended = true;
      }
    }

    if (where != null) {
      if (conditionsAppended) {
        buf.append(" and ");
      }
      buf.append(where);
      conditionsAppended = true;
    }
    iter = whereColumns.entrySet().iterator();
    while (iter.hasNext()) {
      final Map.Entry<String, String> e = iter.next();
      if (conditionsAppended) {
        buf.append(" and ");
      }
      buf.append(e.getKey()).append(e.getValue());
      conditionsAppended = true;
    }
    if (versionColumnName != null) {
      if (conditionsAppended) {
        buf.append(" and ");
      }
      buf.append(versionColumnName).append("=?");
    }

    return buf.toString();
  }
}
