/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;

/**
 * A SQL {@code UPDATE} statement.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("UnusedReturnValue")
public class Update implements StatementSequence {

  protected final String tableName;

  @Nullable
  protected CharSequence comment;

  protected final ArrayList<Restriction> restrictions = new ArrayList<>();

  protected final LinkedHashMap<String, String> assignments = new LinkedHashMap<>();

  public Update(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
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
    restrictions.add(Restriction.equal(column));
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
    restrictions.add(Restriction.equal(column, value));
    return this;
  }

  public Update addColumnIsNullRestriction(String columnName) {
    restrictions.add(Restriction.isNull(columnName));
    return this;
  }

  public Update addColumnIsNotNullRestriction(String columnName) {
    restrictions.add(Restriction.isNotNull(columnName));
    return this;
  }

  public ArrayList<Restriction> getRestrictions() {
    return restrictions;
  }

  @Override
  public String toStatementString(Platform platform) {
    final var buf = new StringBuilder((assignments.size() * 15) + tableName.length() + 10);

    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    buf.append("UPDATE ").append(tableName);
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

    Restriction.render(restrictions, buf);
    return buf.toString();
  }

}
