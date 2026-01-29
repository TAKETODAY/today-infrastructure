/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;

/**
 * An SQL {@code DELETE} statement
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("UnusedReturnValue")
public class Delete implements StatementSequence {

  protected final String tableName;

  @Nullable
  protected CharSequence comment;

  protected final ArrayList<Restriction> restrictions = new ArrayList<>();

  public Delete(String tableName) {
    this.tableName = tableName;
  }

  public Delete setComment(@Nullable CharSequence comment) {
    this.comment = comment;
    return this;
  }

  public Delete addColumnRestriction(String columnName) {
    restrictions.add(Restriction.equal(columnName));
    return this;
  }

  public Delete addColumnRestriction(String... columnNames) {
    for (String columnName : columnNames) {
      if (columnName == null) {
        continue;
      }
      addColumnRestriction(columnName);
    }
    return this;
  }

  public Delete addColumnIsNullRestriction(String columnName) {
    restrictions.add(Restriction.isNull(columnName));
    return this;
  }

  public Delete addColumnIsNotNullRestriction(String columnName) {
    restrictions.add(Restriction.isNotNull(columnName));
    return this;
  }

  public Delete setVersionColumnName(@Nullable String versionColumnName) {
    if (versionColumnName != null) {
      addColumnRestriction(versionColumnName);
    }
    return this;
  }

  @Override
  public String toStatementString(Platform platform) {
    final StringBuilder buf = new StringBuilder(tableName.length() + 10);

    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    buf.append("DELETE FROM ").append(tableName);

    Restriction.render(restrictions, buf);

    return buf.toString();
  }

}