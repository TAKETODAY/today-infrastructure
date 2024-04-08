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
import java.util.Iterator;

import cn.taketoday.core.Pair;
import cn.taketoday.jdbc.persistence.StatementSequence;
import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.lang.Nullable;

/**
 * An SQL <tt>INSERT</tt> statement
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Insert implements StatementSequence {

  protected final String tableName;

  @Nullable
  protected String comment;

  public final ArrayList<Pair<String, String>> columns = new ArrayList<>();

  public Insert(String tableName) {
    this.tableName = tableName;
  }

  public Insert setComment(@Nullable String comment) {
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

  public Insert addColumn(String columnName, String valueExpression) {
    columns.add(Pair.of(columnName, valueExpression));
    return this;
  }

  @Override
  public String toStatementString(Platform platform) {
    final StringBuilder buf = new StringBuilder(columns.size() * 15 + tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    buf.append("INSERT INTO ").append(tableName);

    if (columns.isEmpty()) {
      buf.append(' ').append(platform.getNoColumnsInsertString());
    }
    else {
      buf.append(" (");
      renderInsertionSpec(buf);
      buf.append(") VALUES (");
      renderRowValues(buf);
      buf.append(')');
    }
    return buf.toString();
  }

  private void renderInsertionSpec(StringBuilder buf) {
    buf.append('`');
    final Iterator<Pair<String, String>> itr = columns.iterator();
    while (itr.hasNext()) {
      buf.append(itr.next().first).append('`');
      if (itr.hasNext()) {
        buf.append(", `");
      }
    }
  }

  private void renderRowValues(StringBuilder buf) {
    final Iterator<Pair<String, String>> itr = columns.iterator();
    while (itr.hasNext()) {
      buf.append(itr.next().second);
      if (itr.hasNext()) {
        buf.append(", ");
      }
    }
  }

}
