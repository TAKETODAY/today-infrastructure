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
import infra.persistence.platform.Platform;
import infra.persistence.query.StatementSequence;

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

  protected OrderSpec.Builder orderByBuilder = OrderSpec.builder();

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

  /**
   * Replace both the limit and offset with the supplied pagination.
   *
   * @param pageable the pagination to apply
   * @return this select
   * @throws IllegalArgumentException if the limit or offset is negative
   */
  public SimpleSelect pageable(Pageable pageable) {
    int limit = pageable.pageSize();
    int offset = pageable.offset();
    if (limit < 0 || offset < 0) {
      throw new IllegalArgumentException("Limit and offset must not be negative");
    }
    this.limit = limit;
    this.offset = offset;
    return this;
  }

  /**
   * Set the maximum row count, leaving the offset unchanged.
   *
   * @param limit a non-negative row count, or {@code null} to remove the limit
   * @return this select
   * @throws IllegalArgumentException if the limit is negative
   */
  public SimpleSelect limit(@Nullable Integer limit) {
    if (limit != null && limit < 0) {
      throw new IllegalArgumentException("Limit must not be negative");
    }
    this.limit = limit;
    return this;
  }

  /**
   * Set the number of rows to skip, independently of the limit.
   *
   * @param offset a non-negative offset, or {@code null} to remove the offset
   * @return this select
   * @throws IllegalArgumentException if the offset is negative
   */
  public SimpleSelect offset(@Nullable Integer offset) {
    if (offset != null && offset < 0) {
      throw new IllegalArgumentException("Offset must not be negative");
    }
    this.offset = offset;
    return this;
  }

  /**
   * Clear both the limit and offset.
   *
   * @return this select
   * @since 5.0
   */
  public SimpleSelect clearPagination() {
    this.limit = null;
    this.offset = null;
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
      restrictions.add(Restrictions.plain(condition));
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
    restrictions.add(Restrictions.equal(columnName));
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

  /**
   * Replace the ORDER BY with the given spec, discarding any parts previously
   * accumulated through {@link #orderBy()}.
   *
   * @param orderSpec the spec to use; {@code null} clears the ordering
   */
  public SimpleSelect orderBy(@Nullable OrderSpec orderSpec) {
    this.orderByBuilder = orderSpec != null ? orderSpec.mutate() : OrderSpec.builder();
    return this;
  }

  /**
   * Replace the ORDER BY with the given spec, discarding any parts previously
   * accumulated through {@link #orderBy()}.
   *
   * @param orderSpec the spec to use; {@code null} clears the ordering
   */
  public SimpleSelect orderBy(OrderSpec.@Nullable Builder orderSpec) {
    this.orderByBuilder = orderSpec != null ? orderSpec : OrderSpec.builder();
    return this;
  }

  /**
   * Return the builder backing this select's ordering, for incrementally appending
   * sort keys or raw fragments. Appending continues after any parts already present.
   *
   * @return the builder backing this select's ordering
   */
  public OrderSpec.Builder orderBy() {
    return orderByBuilder;
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
    buf.append(" FROM ");
    tableName.appendTo(buf, platform);
    // where
    Restrictions.append(platform, restrictions, buf);

    if (!orderByBuilder.isEmpty()) {
      buf.append(" order by ");
      orderByBuilder.build().appendTo(buf, platform);
    }

    platform.appendPagination(buf, limit, offset);
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

        col.appendTo(buf, platform);
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
