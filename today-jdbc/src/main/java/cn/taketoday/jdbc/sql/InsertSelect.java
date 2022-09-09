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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.jdbc.sql.dialect.Dialect;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * Implementation of InsertSelect.
 * <p> from hibernate
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class InsertSelect {

  protected String tableName;
  protected String comment;

  protected List<String> columnNames = new ArrayList<>();

  protected Select select;

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

  public String toStatementString() {
    Assert.state(select != null, "no select defined for insert-select");
    Assert.state(tableName != null, "no table name defined for insert-select");

    StringBuilder buf = new StringBuilder((columnNames.size() * 15) + tableName.length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Dialect.escapeComment(comment)).append(" */ ");
    }
    buf.append("insert into ").append(tableName);
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
    buf.append(' ').append(select.toStatementString());
    return buf.toString();
  }
}
