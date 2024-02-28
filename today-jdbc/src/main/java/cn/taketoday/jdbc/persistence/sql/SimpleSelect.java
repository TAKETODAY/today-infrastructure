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
import java.util.HashMap;
import java.util.HashSet;

import cn.taketoday.jdbc.persistence.StatementSequence;
import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A SQL {@code SELECT} statement with no table joins.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleSelect implements StatementSequence {

  protected String tableName;

  @Nullable
  protected CharSequence orderByClause;

  @Nullable
  protected CharSequence comment;

  protected final ArrayList<String> columns = new ArrayList<>();

  @Nullable
  protected HashMap<String, String> aliases;

  protected final ArrayList<Restriction> restrictions = new ArrayList<>();

  /**
   * Sets the name of the table we are selecting from
   */
  public SimpleSelect setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Adds selections
   */
  public SimpleSelect addColumns(String[] columnNames) {
    CollectionUtils.addAll(this.columns, columnNames);
    return this;
  }

  /**
   * Adds a selection
   */
  public SimpleSelect addColumn(String columnName) {
    columns.add(columnName);
    return this;
  }

  /**
   * Adds a selection, with an alias
   */
  public SimpleSelect addColumn(String columnName, String alias) {
    columns.add(columnName);
    if (aliases == null) {
      aliases = new HashMap<>();
    }
    aliases.put(columnName, alias);
    return this;
  }

  /**
   * Appends a complete where condition.
   * The {@code condition} is added as-is.
   */
  public SimpleSelect addWhereToken(String condition) {
    if (condition != null) {
      restrictions.add(new CompleteRestriction(condition));
    }
    return this;
  }

  /**
   * Appends a restriction comparing the {@code columnName} for equality with a parameter
   *
   * @see #addRestriction(String, ComparisonRestriction.Operator, String)
   */
  public SimpleSelect addRestriction(String columnName) {
    restrictions.add(new ComparisonRestriction(columnName));
    return this;
  }

  /**
   * Appends a restriction based on the comparison between {@code lhs} and {@code rhs}.
   */
  public SimpleSelect addRestriction(String lhs, ComparisonRestriction.Operator op, String rhs) {
    restrictions.add(new ComparisonRestriction(lhs, op, rhs));
    return this;
  }

  /**
   * Appends a restriction comparing each name in {@code columnNames} for equality with a parameter
   *
   * @see #addRestriction(String)
   */
  public SimpleSelect addRestriction(String... columnNames) {
    for (String columnName : columnNames) {
      if (columnName != null) {
        addRestriction(columnName);
      }
    }
    return this;
  }

  public SimpleSelect addRestriction(Restriction restriction) {
    restrictions.add(restriction);
    return this;
  }

  public SimpleSelect setOrderByClause(@Nullable CharSequence orderByClause) {
    this.orderByClause = orderByClause;
    return this;
  }

  public SimpleSelect setComment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public String toStatementString() {
    final var buf = new StringBuilder(columns.size() * 10 + tableName.length() + restrictions.size() * 10 + 10);

    applyComment(buf);
    applySelectClause(buf);
    applyFromClause(buf);
    applyWhereClause(buf);
    applyOrderBy(buf);

    return buf.toString();
  }

  private void applyComment(StringBuilder buf) {
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
  }

  private void applySelectClause(StringBuilder buf) {
    buf.append("SELECT ");

    boolean appendComma = false;
    final HashSet<String> uniqueColumns = new HashSet<>();
    for (final String col : columns) {
      final String alias = getAlias(col);
      if (uniqueColumns.add(alias == null ? col : alias)) {
        if (appendComma) {
          buf.append(", `");
        }
        else {
          appendComma = true;
          buf.append('`');
        }
        buf.append(col);

        if (alias != null && !alias.equals(col)) {
          buf.append("` AS ").append(alias);
        }
        else {
          buf.append('`');
        }
      }
    }
  }

  @Nullable
  private String getAlias(String col) {
    if (aliases == null) {
      return null;
    }
    return aliases.get(col);
  }

  private void applyFromClause(StringBuilder buf) {
    buf.append(" FROM ").append(tableName);
  }

  private void applyWhereClause(StringBuilder buf) {
    Restriction.render(restrictions, buf);
  }

  private void applyOrderBy(StringBuilder buf) {
    if (orderByClause != null) {
      buf.append(" order by ").append(orderByClause);
    }
  }

}
