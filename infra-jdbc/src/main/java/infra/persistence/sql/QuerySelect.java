/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

import java.util.Iterator;
import java.util.Set;

import infra.persistence.StatementSequence;
import infra.persistence.platform.Platform;

/**
 * A translated HQL query
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class QuerySelect implements StatementSequence {

  private static final Set<String> DONT_SPACE_TOKENS = Set.of(
          ".", "+", "-", "/", "*", "<", ">",
          "=", "#", "~", "|", "&", "<=", ">=",
          "=>", "=<", "!=", "<>", "!#", "!~",
          "!<", "!>", "(", /*for MySQL*/ ")");

  private final StringBuilder select = new StringBuilder();

  private final StringBuilder where = new StringBuilder();

  private final StringBuilder groupBy = new StringBuilder();

  private final StringBuilder orderBy = new StringBuilder();

  private final StringBuilder having = new StringBuilder();

  private String comment;

  private boolean distinct;

  private final JoinFragment joins;

  @SuppressWarnings("NullAway")
  public QuerySelect(Platform platform) {
    joins = new QueryJoinFragment(platform, false);
  }

  public JoinFragment getJoinFragment() {
    return joins;
  }

  public void addSelectFragmentString(String fragment) {
    if (!fragment.isEmpty() && fragment.charAt(0) == ',') {
      fragment = fragment.substring(1);
    }
    fragment = fragment.trim();
    if (!fragment.isEmpty()) {
      if (!select.isEmpty()) {
        select.append(", ");
      }
      select.append(fragment);
    }
  }

  public void addSelectColumn(String columnName, String alias) {
    addSelectFragmentString(columnName + ' ' + alias);
  }

  public void setDistinct(boolean distinct) {
    this.distinct = distinct;
  }

  public void setWhereTokens(Iterator<String> tokens) {
    //if ( conjunctiveWhere.length()>0 ) conjunctiveWhere.append(" and ");
    appendTokens(where, tokens);
  }

  public void prependWhereConditions(String conditions) {
    if (!where.isEmpty()) {
      where.insert(0, conditions + " and ");
    }
    else {
      where.append(conditions);
    }
  }

  public void setGroupByTokens(Iterator<String> tokens) {
    //if ( groupBy.length()>0 ) groupBy.append(" and ");
    appendTokens(groupBy, tokens);
  }

  public void setOrderByTokens(Iterator<String> tokens) {
    //if ( orderBy.length()>0 ) orderBy.append(" and ");
    appendTokens(orderBy, tokens);
  }

  public void setHavingTokens(Iterator<String> tokens) {
    //if ( having.length()>0 ) having.append(" and ");
    appendTokens(having, tokens);
  }

  public void addOrderBy(String orderByString) {
    if (!orderBy.isEmpty()) {
      orderBy.append(", ");
    }
    orderBy.append(orderByString);
  }

  @Override
  public String toStatementString(Platform platform) {
    StringBuilder buf = new StringBuilder(50);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
    buf.append("SELECT ");
    if (distinct) {
      buf.append("DISTINCT ");
    }
    String from = joins.toFromFragmentString();
    if (from.startsWith(",")) {
      from = from.substring(1);
    }
    else if (from.startsWith(" INNER JOIN")) {
      from = from.substring(11);
    }

    buf.append(select).append(" FROM ").append(from);

    String outerJoinsAfterWhere = joins.toWhereFragmentString().trim();
    String whereConditions = where.toString().trim();
    boolean hasOuterJoinsAfterWhere = !outerJoinsAfterWhere.isEmpty();
    boolean hasWhereConditions = !whereConditions.isEmpty();
    if (hasOuterJoinsAfterWhere || hasWhereConditions) {
      buf.append(" WHERE ");
      if (hasOuterJoinsAfterWhere) {
        buf.append(outerJoinsAfterWhere.substring(4));
      }
      if (hasWhereConditions) {
        if (hasOuterJoinsAfterWhere) {
          buf.append(" AND (");
        }
        buf.append(whereConditions);
        if (hasOuterJoinsAfterWhere) {
          buf.append(")");
        }
      }
    }

    if (!groupBy.isEmpty()) {
      buf.append(" GROUP BY ").append(groupBy);
    }
    if (!having.isEmpty()) {
      buf.append(" HAVING ").append(having);
    }
    if (!orderBy.isEmpty()) {
      buf.append(" order by ").append(orderBy);
    }

    return buf.toString();
  }

  private static void appendTokens(StringBuilder buf, Iterator<String> iter) {
    boolean lastSpaceable = true;
    boolean lastQuoted = false;
    while (iter.hasNext()) {
      String token = iter.next();
      boolean spaceable = !DONT_SPACE_TOKENS.contains(token);
      boolean quoted = token.startsWith("'");
      if (spaceable && lastSpaceable) {
        if (!quoted || !lastQuoted) {
          buf.append(' ');
        }
      }
      lastSpaceable = spaceable;
      buf.append(token);
      lastQuoted = token.endsWith("'");
    }
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

}
