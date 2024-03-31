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

import cn.taketoday.jdbc.persistence.StatementSequence;
import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.lang.Nullable;

/**
 * A simple SQL <tt>SELECT</tt> statement
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class Select implements StatementSequence {

  protected CharSequence selectClause;

  protected CharSequence fromClause;

  @Nullable
  protected CharSequence outerJoinsAfterFrom;

  @Nullable
  protected CharSequence whereClause;

  @Nullable
  protected CharSequence outerJoinsAfterWhere;

  @Nullable
  protected CharSequence orderByClause;

  @Nullable
  protected CharSequence groupByClause;

  @Nullable
  protected CharSequence comment;

  protected boolean forUpdate;

  private int guesstimatedBufferSize = 20;

  /**
   * Construct an SQL <tt>SELECT</tt> statement from the given clauses
   */
  @Override
  public String toStatementString(Platform platform) {
    StringBuilder buf = new StringBuilder(guesstimatedBufferSize);

    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }

    buf.append("SELECT ").append(selectClause)
            .append(" FROM ").append(fromClause);

    if (outerJoinsAfterFrom != null) {
      buf.append(outerJoinsAfterFrom);
    }

    if (whereClause != null || outerJoinsAfterWhere != null) {
      buf.append(" WHERE ");
      // the outerJoinsAfterWhere needs to come before where clause to properly
      // handle dynamic filters
      if (outerJoinsAfterWhere != null) {
        buf.append(outerJoinsAfterWhere);
        if (whereClause != null && !whereClause.isEmpty()) {
          buf.append(" AND ");
        }
      }
      if (whereClause != null && !whereClause.isEmpty()) {
        buf.append(whereClause);
      }
    }

    if (groupByClause != null) {
      buf.append(" group by ").append(groupByClause);
    }

    if (orderByClause != null) {
      buf.append(" order by ").append(orderByClause);
    }

    if (forUpdate) {
      buf.append(platform.getForUpdateString());
    }

    return buf.toString();
  }

  /**
   * Sets the fromClause.
   *
   * @param fromClause The fromClause to set
   */
  public Select setFromClause(CharSequence fromClause) {
    this.fromClause = fromClause;
    this.guesstimatedBufferSize += fromClause.length();
    return this;
  }

  public Select setFromClause(String tableName, String alias) {
    this.fromClause = tableName + ' ' + alias;
    this.guesstimatedBufferSize += fromClause.length();
    return this;
  }

  public Select setOrderByClause(CharSequence orderByClause) {
    this.orderByClause = orderByClause;
    this.guesstimatedBufferSize += orderByClause.length();
    return this;
  }

  public Select setGroupByClause(CharSequence groupByClause) {
    this.groupByClause = groupByClause;
    this.guesstimatedBufferSize += groupByClause.length();
    return this;
  }

  public Select setOuterJoins(CharSequence outerJoinsAfterFrom, String outerJoinsAfterWhere) {
    this.outerJoinsAfterFrom = outerJoinsAfterFrom;

    // strip off any leading 'and' token
    String tmpOuterJoinsAfterWhere = outerJoinsAfterWhere.trim();
    if (tmpOuterJoinsAfterWhere.startsWith("and")) {
      tmpOuterJoinsAfterWhere = tmpOuterJoinsAfterWhere.substring(4);
    }
    this.outerJoinsAfterWhere = tmpOuterJoinsAfterWhere;

    this.guesstimatedBufferSize += outerJoinsAfterFrom.length() + outerJoinsAfterWhere.length();
    return this;
  }

  /**
   * Sets the selectClause.
   *
   * @param selectClause The selectClause to set
   */
  public Select setSelectClause(CharSequence selectClause) {
    this.selectClause = selectClause;
    this.guesstimatedBufferSize += selectClause.length();
    return this;
  }

  public Select setSelectClause(SelectFragment selectFragment) {
    setSelectClause(selectFragment.toFragmentString().substring(2));
    return this;
  }

  /**
   * Sets the whereClause.
   *
   * @param whereClause The whereClause to set
   */
  public Select setWhereClause(@Nullable CharSequence whereClause) {
    if (this.whereClause != null) {
      this.guesstimatedBufferSize -= this.whereClause.length();
    }
    if (whereClause != null) {
      this.guesstimatedBufferSize += whereClause.length();
    }
    this.whereClause = whereClause;
    return this;
  }

  public Select setComment(CharSequence comment) {
    this.comment = comment;
    this.guesstimatedBufferSize += comment.length();
    return this;
  }

  public Select setForUpdate(boolean forUpdate) {
    this.forUpdate = forUpdate;
    return this;
  }

}
