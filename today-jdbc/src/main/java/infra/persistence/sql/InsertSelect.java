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

package infra.persistence.sql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import infra.lang.Assert;
import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;
import infra.util.CollectionUtils;

/**
 * Implementation of InsertSelect.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InsertSelect implements StatementSequence {

  protected String tableName;

  protected String comment;

  protected List<String> columnNames = new ArrayList<>();

  protected Select select;

  @SuppressWarnings("NullAway")
  public InsertSelect() {
  }

  public InsertSelect setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public InsertSelect setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public InsertSelect addColumn(String columnName) {
    columnNames.add(columnName);
    return this;
  }

  public InsertSelect addColumns(String[] columnNames) {
    CollectionUtils.addAll(this.columnNames, columnNames);
    return this;
  }

  public InsertSelect setSelect(Select select) {
    this.select = select;
    return this;
  }

  @Override
  public String toStatementString(Platform platform) {
    Assert.state(select != null, "no select defined for insert-select");
    Assert.state(tableName != null, "no table name defined for insert-select");

    StringBuilder buf = new StringBuilder((columnNames.size() * 15) + tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
    buf.append("INSERT INTO ").append(tableName);
    if (!columnNames.isEmpty()) {
      buf.append(" (");
      Iterator<String> itr = columnNames.iterator();
      while (itr.hasNext()) {
        buf.append(itr.next());
        if (itr.hasNext()) {
          buf.append(", ");
        }
      }
      buf.append(")");
    }
    buf.append(' ').append(select.toStatementString(platform));
    return buf.toString();
  }
}
