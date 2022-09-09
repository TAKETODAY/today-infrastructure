/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.sql;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.jdbc.sql.dialect.Dialect;

/**
 * An SQL {@code DELETE} statement
 * <p> from hibernate
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class Delete {

  protected String tableName;
  protected String versionColumnName;
  protected String where;
  protected String comment;

  protected Map<String, String> primaryKeyColumns = new LinkedHashMap<>();

  public Delete setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public Delete setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public String toStatementString() {
    StringBuilder buf = new StringBuilder(tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Dialect.escapeComment(comment)).append(" */ ");
    }
    buf.append("delete from ").append(tableName);
    if (where != null || !primaryKeyColumns.isEmpty() || versionColumnName != null) {
      buf.append(" where ");
    }
    boolean conditionsAppended = false;
    Iterator<Map.Entry<String, String>> iter = primaryKeyColumns.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, String> e = iter.next();
      buf.append(e.getKey()).append('=').append(e.getValue());
      if (iter.hasNext()) {
        buf.append(" and ");
      }
      conditionsAppended = true;
    }
    if (where != null) {
      if (conditionsAppended) {
        buf.append(" and ");
      }
      buf.append(where);
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

  public Delete setWhere(String where) {
    this.where = where;
    return this;
  }

  public Delete addWhereFragment(String fragment) {
    if (where == null) {
      where = fragment;
    }
    else {
      where += (" and " + fragment);
    }
    return this;
  }

  public Delete setPrimaryKeyColumnNames(String[] columnNames) {
    this.primaryKeyColumns.clear();
    addPrimaryKeyColumns(columnNames);
    return this;
  }

  public Delete addPrimaryKeyColumns(String[] columnNames) {
    for (String columnName : columnNames) {
      addPrimaryKeyColumn(columnName, "?");
    }
    return this;
  }

  public Delete addPrimaryKeyColumns(String[] columnNames, boolean[] includeColumns, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      if (includeColumns[i]) {
        addPrimaryKeyColumn(columnNames[i], valueExpressions[i]);
      }
    }
    return this;
  }

  public Delete addPrimaryKeyColumns(String[] columnNames, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      addPrimaryKeyColumn(columnNames[i], valueExpressions[i]);
    }
    return this;
  }

  public Delete addPrimaryKeyColumn(String columnName, String valueExpression) {
    this.primaryKeyColumns.put(columnName, valueExpression);
    return this;
  }

  public Delete setVersionColumnName(String versionColumnName) {
    this.versionColumnName = versionColumnName;
    return this;
  }

}