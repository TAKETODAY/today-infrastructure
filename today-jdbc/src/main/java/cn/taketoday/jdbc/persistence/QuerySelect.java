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


package cn.taketoday.jdbc.persistence;

import java.util.HashSet;
import java.util.Iterator;

import cn.taketoday.jdbc.persistence.dialect.Platform;

/**
 * A translated HQL query
 * <p> from hibernate
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class QuerySelect {

  private final Platform platform;
  private JoinFragment joins;
  private final StringBuilder select = new StringBuilder();
  private final StringBuilder where = new StringBuilder();
  private final StringBuilder groupBy = new StringBuilder();
  private final StringBuilder orderBy = new StringBuilder();
  private final StringBuilder having = new StringBuilder();
  private String comment;
  private boolean distinct;

  private static final HashSet<String> DONT_SPACE_TOKENS = new HashSet<String>();

  static {
    //dontSpace.add("'");
    DONT_SPACE_TOKENS.add(".");
    DONT_SPACE_TOKENS.add("+");
    DONT_SPACE_TOKENS.add("-");
    DONT_SPACE_TOKENS.add("/");
    DONT_SPACE_TOKENS.add("*");
    DONT_SPACE_TOKENS.add("<");
    DONT_SPACE_TOKENS.add(">");
    DONT_SPACE_TOKENS.add("=");
    DONT_SPACE_TOKENS.add("#");
    DONT_SPACE_TOKENS.add("~");
    DONT_SPACE_TOKENS.add("|");
    DONT_SPACE_TOKENS.add("&");
    DONT_SPACE_TOKENS.add("<=");
    DONT_SPACE_TOKENS.add(">=");
    DONT_SPACE_TOKENS.add("=>");
    DONT_SPACE_TOKENS.add("=<");
    DONT_SPACE_TOKENS.add("!=");
    DONT_SPACE_TOKENS.add("<>");
    DONT_SPACE_TOKENS.add("!#");
    DONT_SPACE_TOKENS.add("!~");
    DONT_SPACE_TOKENS.add("!<");
    DONT_SPACE_TOKENS.add("!>");
    DONT_SPACE_TOKENS.add("("); //for MySQL
    DONT_SPACE_TOKENS.add(")");
  }

  public QuerySelect(Platform platform) {
    this.platform = platform;
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

  public String toQueryString() {
    StringBuilder buf = new StringBuilder(50);
    if (comment != null) {
      buf.append("/* ").append(Platform.escapeComment(comment)).append(" */ ");
    }
    buf.append("select ");
    if (distinct) {
      buf.append("distinct ");
    }
    String from = joins.toFromFragmentString();
    if (from.startsWith(",")) {
      from = from.substring(1);
    }
    else if (from.startsWith(" inner join")) {
      from = from.substring(11);
    }

    buf.append(select)
            .append(" from")
            .append(from);

    String outerJoinsAfterWhere = joins.toWhereFragmentString().trim();
    String whereConditions = where.toString().trim();
    boolean hasOuterJoinsAfterWhere = !outerJoinsAfterWhere.isEmpty();
    boolean hasWhereConditions = !whereConditions.isEmpty();
    if (hasOuterJoinsAfterWhere || hasWhereConditions) {
      buf.append(" where ");
      if (hasOuterJoinsAfterWhere) {
        buf.append(outerJoinsAfterWhere.substring(4));
      }
      if (hasWhereConditions) {
        if (hasOuterJoinsAfterWhere) {
          buf.append(" and (");
        }
        buf.append(whereConditions);
        if (hasOuterJoinsAfterWhere) {
          buf.append(")");
        }
      }
    }

    if (!groupBy.isEmpty()) {
      buf.append(" group by ").append(groupBy);
    }
    if (!having.isEmpty()) {
      buf.append(" having ").append(having);
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

  public QuerySelect copy() {
    QuerySelect copy = new QuerySelect(platform);
    copy.joins = this.joins.copy();
    copy.select.append(this.select);
    copy.where.append(this.where);
    copy.groupBy.append(this.groupBy);
    copy.orderBy.append(this.orderBy);
    copy.having.append(this.having);
    copy.comment = this.comment;
    copy.distinct = this.distinct;
    return copy;
  }

}
