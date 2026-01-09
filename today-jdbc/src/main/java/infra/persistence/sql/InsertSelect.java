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
