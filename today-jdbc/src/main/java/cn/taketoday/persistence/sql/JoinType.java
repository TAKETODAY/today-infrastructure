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

package cn.taketoday.persistence.sql;

/**
 * @author Strong Liu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
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
