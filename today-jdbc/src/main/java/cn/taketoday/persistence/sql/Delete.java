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

package cn.taketoday.persistence.sql;

import java.util.ArrayList;

import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.StatementSequence;
import cn.taketoday.persistence.dialect.Platform;

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