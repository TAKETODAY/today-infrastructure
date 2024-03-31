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

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A restriction (predicate) to be applied to a query
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Restriction {

  /**
   * Render the restriction into the SQL buffer
   */
  void render(StringBuilder sqlBuffer);

  // Static Factory Methods

  static Restriction plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  /**
   * equal
   */
  static Restriction equal(String columnName) {
    return new ComparisonRestriction(columnName, " = ", "?");
  }

  /**
   * equal
   */
  static Restriction equal(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " = ", rhs);
  }

  /**
   * not equal
   */
  static Restriction notEqual(String columnName) {
    return new ComparisonRestriction(columnName, " <> ", "?");
  }

  /**
   * not equal
   */
  static Restriction notEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <> ", rhs);
  }

  static Restriction graterThan(String columnName) {
    return graterThan(columnName, "?");
  }

  static Restriction graterThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  static Restriction graterEqual(String columnName) {
    return graterEqual(columnName, "?");
  }

  static Restriction graterEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " >= ", rhs);
  }

  static Restriction lessThan(String columnName) {
    return lessThan(columnName, "?");
  }

  static Restriction lessThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " < ", rhs);
  }

  static Restriction lessEqual(String columnName) {
    return lessEqual(columnName, "?");
  }

  static Restriction lessEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <= ", rhs);
  }

  static Restriction forOperator(String lhs, String operator, String rhs) {
    return new ComparisonRestriction(lhs, operator, rhs);
  }

  /**
   * Null-ness restriction - IS (NOT)? NULL
   */
  static Restriction isNull(String columnName) {
    return new NullnessRestriction(columnName, true);
  }

  /**
   * Null-ness restriction - IS (NOT)? NULL
   */
  static Restriction isNotNull(String columnName) {
    return new NullnessRestriction(columnName, false);
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void render(@Nullable Collection<? extends Restriction> restrictions, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      renderWhereClause(restrictions, buf);
    }
  }

  /**
   * Render the restriction into the SQL buffer
   */
  @Nullable
  static StringBuilder renderWhereClause(@Nullable Collection<? extends Restriction> restrictions) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      StringBuilder buf = new StringBuilder(restrictions.size() * 10);
      renderWhereClause(restrictions, buf);
      return buf;
    }
    return null;
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void renderWhereClause(Collection<? extends Restriction> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        buf.append(" AND ");
      }
      else {
        appended = true;
      }
      restriction.render(buf);
    }
  }

  class Plain implements Restriction {

    private final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public void render(StringBuilder sqlBuffer) {
      sqlBuffer.append(sequence);
    }

  }

}
