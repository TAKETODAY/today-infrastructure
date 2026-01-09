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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import infra.persistence.Order;
import infra.persistence.Pageable;
import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;
import infra.util.CollectionUtils;

/**
 * A SQL {@code SELECT} statement with no table joins.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleSelect implements StatementSequence {

  public final List<Restriction> restrictions;

  protected final List<String> columns;

  protected String tableName;

  @Nullable
  protected OrderByClause orderByClause;

  @Nullable
  protected CharSequence comment;

  @Nullable
  protected HashMap<String, String> aliases;

  @Nullable
  private Integer limit;

  @Nullable
  private Integer offset;

  @SuppressWarnings("NullAway")
  public SimpleSelect() {
    this.columns = new ArrayList<>();
    this.restrictions = new ArrayList<>();
  }

  @SuppressWarnings("NullAway")
  public SimpleSelect(List<String> columns, List<Restriction> restrictions) {
    this.restrictions = restrictions;
    this.columns = columns;
  }

  public SimpleSelect pageable(Pageable pageable) {
    this.limit = pageable.pageSize();
    this.offset = pageable.offset();
    return this;
  }

  public SimpleSelect limit(@Nullable Integer limit) {
    this.limit = limit;
    return this;
  }

  public SimpleSelect offset(@Nullable Integer offset) {
    this.offset = offset;
    return this;
  }

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
  public SimpleSelect addWhereToken(CharSequence condition) {
    if (condition != null) {
      restrictions.add(Restriction.plain(condition));
    }
    return this;
  }

  /**
   * Appends a restriction comparing the {@code columnName} for equality with a parameter
   */
  public SimpleSelect addRestriction(String columnName) {
    restrictions.add(Restriction.equal(columnName));
    return this;
  }

  /**
   * Appends a restriction comparing each name in {@code columnNames} for equality with a parameter
   *
   * @see #addRestriction(String)
   */
  public SimpleSelect addRestrictions(String... columnNames) {
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

  public SimpleSelect orderBy(String col) {
    orderBy().asc(col);
    return this;
  }

  public SimpleSelect orderBy(String col, Order order) {
    orderBy().orderBy(col, order);
    return this;
  }

  public SimpleSelect orderBy(@Nullable OrderByClause orderByClause) {
    this.orderByClause = orderByClause;
    return this;
  }

  public MutableOrderByClause orderBy() {
    if (orderByClause instanceof MutableOrderByClause mutable) {
      return mutable;
    }
    var mutable = OrderByClause.mutable();
    this.orderByClause = mutable;
    return mutable;
  }

  public SimpleSelect setComment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public String toStatementString(Platform platform) {
    StringBuilder buf = new StringBuilder(columns.size() * 10 + tableName.length() + restrictions.size() * 10 + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    applySelectClause(buf);
    buf.append(" FROM ").append(tableName);
    // where
    Restriction.render(restrictions, buf);

    OrderByClause orderByClause = this.orderByClause;
    if (orderByClause != null && !orderByClause.isEmpty()) {
      buf.append(" order by ").append(orderByClause.toClause());
    }

    if (limit != null) {
      buf.append(" LIMIT ").append(limit);
      if (offset != null && offset > 0) {
        buf.append(" OFFSET ").append(offset);
      }
    }

    return buf.toString();
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

}
