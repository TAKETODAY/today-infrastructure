/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import infra.persistence.Identifier;
import infra.persistence.Order;
import infra.persistence.Pageable;
import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;

/**
 * A SQL {@code SELECT} statement with no table joins.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleSelect implements StatementSequence {

  public final List<Restriction> restrictions;

  protected final List<Identifier> columns;

  protected Identifier tableName;

  protected @Nullable OrderByClause orderByClause;

  protected @Nullable CharSequence comment;

  protected @Nullable HashMap<Identifier, String> aliases;

  private @Nullable Integer limit;

  private @Nullable Integer offset;

  @SuppressWarnings("NullAway")
  public SimpleSelect() {
    this.columns = new ArrayList<>();
    this.restrictions = new ArrayList<>();
  }

  @SuppressWarnings("NullAway")
  public SimpleSelect(List<Identifier> columns, List<Restriction> restrictions) {
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
    return setTableName(Identifier.parse(tableName));
  }

  /**
   * Sets the name of the table we are selecting from
   */
  public SimpleSelect setTableName(Identifier tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Adds selections
   */
  public SimpleSelect addColumns(String[] columnNames) {
    for (String columnName : columnNames) {
      addColumn(columnName);
    }
    return this;
  }

  /**
   * Adds a selection
   */
  public SimpleSelect addColumn(String columnName) {
    return addColumn(Identifier.parse(columnName));
  }

  /**
   * Adds a selection
   */
  public SimpleSelect addColumn(Identifier columnName) {
    columns.add(columnName);
    return this;
  }

  /**
   * Adds a selection, with an alias
   */
  public SimpleSelect addColumn(String columnName, String alias) {
    return addColumn(Identifier.parse(columnName), alias);
  }

  /**
   * Adds a selection, with an alias
   */
  public SimpleSelect addColumn(Identifier columnName, String alias) {
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
  public SimpleSelect addWhereToken(@Nullable CharSequence condition) {
    if (condition != null) {
      restrictions.add(Restriction.plain(condition));
    }
    return this;
  }

  /**
   * Appends a restriction comparing the {@code columnName} for equality with a parameter
   */
  public SimpleSelect addRestriction(String columnName) {
    return addRestriction(Identifier.parse(columnName));
  }

  /**
   * Appends a restriction comparing the {@code columnName} for equality with a parameter
   */
  public SimpleSelect addRestriction(Identifier columnName) {
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
    StringBuilder buf = new StringBuilder(columns.size() * 10 + tableName.getText().length() + restrictions.size() * 10 + 10);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    applySelectClause(platform, buf);
    buf.append(" FROM ").append(tableName.render(platform));
    // where
    Restriction.append(platform, restrictions, buf);

    OrderByClause orderByClause = this.orderByClause;
    if (orderByClause != null && !orderByClause.isEmpty()) {
      buf.append(" order by ").append(orderByClause.toClause(platform));
    }

    if (limit != null) {
      buf.append(" LIMIT ").append(limit);
      if (offset != null && offset > 0) {
        buf.append(" OFFSET ").append(offset);
      }
    }

    return buf.toString();
  }

  private void applySelectClause(Platform platform, StringBuilder buf) {
    buf.append("SELECT ");

    boolean appendComma = false;
    final HashSet<String> uniqueColumns = new HashSet<>();
    for (final Identifier col : columns) {
      final String alias = getAlias(col);
      if (uniqueColumns.add(alias == null ? col.getText() : alias)) {
        if (appendComma) {
          buf.append(", ");
        }
        else {
          appendComma = true;
        }

        buf.append(col.render(platform));
        if (alias != null && !alias.equals(col.getText())) {
          buf.append(" AS ").append(alias);
        }
      }
    }
  }

  private @Nullable String getAlias(Identifier col) {
    if (aliases == null) {
      return null;
    }
    return aliases.get(col);
  }

}
