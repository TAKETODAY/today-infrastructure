/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;



import infra.core.Pair;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;
import infra.persistence.query.StatementSequence;
import java.util.ArrayList;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

/**
 * An SQL <tt>INSERT</tt> statement
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Insert implements StatementSequence {

  protected final Identifier tableName;

  protected @Nullable String comment;

  public final ArrayList<Pair<Identifier, String>> columns = new ArrayList<>();

  public Insert(String tableName) {
    this(Identifier.parse(tableName));
  }

  public Insert(Identifier tableName) {
    this.tableName = tableName;
  }

  public Insert setComment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  public Insert addColumn(String columnName) {
    return addColumn(Identifier.parse(columnName));
  }

  public Insert addColumn(Identifier columnName) {
    return addColumn(columnName, "?");
  }

  public Insert addColumns(String[] columnNames) {
    for (String columnName : columnNames) {
      addColumn(columnName);
    }
    return this;
  }

  public Insert addColumn(Identifier columnName, String valueExpression) {
    columns.add(Pair.of(columnName, valueExpression));
    return this;
  }

  @Override
  public String toStatementString(Platform platform) {
    final StringBuilder buf = new StringBuilder(columns.size() * 15 + tableName.getText().length() + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    buf.append("INSERT INTO ").append(tableName.render(platform));

    if (columns.isEmpty()) {
      buf.append(' ').append(platform.getNoColumnsInsertString());
    }
    else {
      buf.append(" (");
      renderInsertionSpec(platform, buf);
      buf.append(") VALUES (");
      renderRowValues(buf);
      buf.append(')');
    }
    return buf.toString();
  }

  private void renderInsertionSpec(Platform platform, StringBuilder buf) {
    final Iterator<Pair<Identifier, String>> itr = columns.iterator();
    while (itr.hasNext()) {
      Identifier identifier = itr.next().first;
      buf.append(identifier.render(platform));
      if (itr.hasNext()) {
        buf.append(", ");
      }
    }
  }

  private void renderRowValues(StringBuilder buf) {
    final Iterator<Pair<Identifier, String>> itr = columns.iterator();
    while (itr.hasNext()) {
      buf.append(itr.next().second);
      if (itr.hasNext()) {
        buf.append(", ");
      }
    }
  }

}
