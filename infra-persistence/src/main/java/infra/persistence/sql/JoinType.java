/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

/**
 * @author Strong Liu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("NullAway")
public enum JoinType {
  NONE(-666, null),
  INNER_JOIN(0, "inner"),
  LEFT_OUTER_JOIN(1, "left"),
  RIGHT_OUTER_JOIN(2, "right"),
  FULL_JOIN(4, "full");

  private final int joinTypeValue;
  private final String sqlText;

  JoinType(int joinTypeValue, String sqlText) {
    this.joinTypeValue = joinTypeValue;
    this.sqlText = sqlText;
  }

  public int getJoinTypeValue() {
    return joinTypeValue;
  }

  public String getSqlText() {
    return sqlText;
  }

  public static JoinType parse(int joinType) {
    if (joinType < 0) {
      return NONE;
    }
    return switch (joinType) {
      case 0 -> INNER_JOIN;
      case 1 -> LEFT_OUTER_JOIN;
      case 2 -> RIGHT_OUTER_JOIN;
      case 4 -> FULL_JOIN;
      default -> throw new IllegalStateException("unknown join type: " + joinType);
    };
  }
}
