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