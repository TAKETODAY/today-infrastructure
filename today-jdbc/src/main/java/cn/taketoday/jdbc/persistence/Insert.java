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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.jdbc.persistence.dialect.Platform;

/**
 * An SQL <tt>INSERT</tt> statement
 * <p> from hibernate
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class Insert {

  protected String tableName;
  protected String comment;

  protected Map<String, String> columns = new LinkedHashMap<>();

  private final Platform platform;

  public Insert(Platform platform) {
    this.platform = platform;
  }

  protected Platform getDialect() {
    return platform;
  }

  public Insert setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public Insert addColumn(String columnName) {
    return addColumn(columnName, "?");
  }

  public Insert addColumns(String[] columnNames) {
    for (String columnName : columnNames) {
      addColumn(columnName);
    }
    return this;
  }

  public Insert addColumns(String[] columnNames, boolean[] insertable) {
    for (int i = 0; i < columnNames.length; i++) {
      if (insertable[i]) {
        addColumn(columnNames[i]);
      }
    }
    return this;
  }

  public Insert addColumns(String[] columnNames, boolean[] insertable, String[] valueExpressions) {
    for (int i = 0; i < columnNames.length; i++) {
      if (insertable[i]) {
        addColumn(columnNames[i], valueExpressions[i]);
      }
    }
    return this;
  }

  public Insert addColumn(String columnName, String valueExpression) {
    columns.put(columnName, valueExpression);
    return this;
  }

  public Insert setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public String toStatementString() {
    StringBuilder buf = new StringBuilder(columns.size() * 15 + tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
    buf.append("insert into ");
    buf.append(tableName);
    if (columns.isEmpty()) {
      buf.append(' ').append(platform.getNoColumnsInsertString());
    }
    else {
      buf.append(" (");
      Iterator<String> iter = columns.keySet().iterator();
      while (iter.hasNext()) {
        buf.append(iter.next());
        if (iter.hasNext()) {
          buf.append(", ");
        }
      }
      buf.append(") values (");
      iter = columns.values().iterator();
      while (iter.hasNext()) {
        buf.append(iter.next());
        if (iter.hasNext()) {
          buf.append(", ");
        }
      }
      buf.append(')');
    }
    return buf.toString();
  }

}
