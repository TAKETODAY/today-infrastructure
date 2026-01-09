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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

import infra.core.Pair;
import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;

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
