/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.sql;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 23:04
 */
public interface Operator {

  Operator GRATE_THAN = plain(" > ?");
  Operator GRATE_EQUALS = plain(" >= ?");
  Operator LESS_THAN = plain(" < ?");
  Operator LESS_EQUALS = plain(" <= ?");

  Operator EQUALS = plain(" = ?");
  Operator NOT_EQUALS = plain(" <> ?");

  Operator IS_NULL = plain(" is null");
  Operator IS_NOT_NULL = plain(" is not null");

  Operator LIKE = plain(" like concat('%', ?, '%')");
  Operator SUFFIX_LIKE = plain(" like concat('%', ?)");
  Operator PREFIX_LIKE = plain(" like concat(?, '%')");

  Operator BETWEEN = plain(" BETWEEN ? AND ?");
  Operator NOT_BETWEEN = plain(" NOT BETWEEN ? AND ?");

  Operator IN = in(false);
  Operator NOT_IN = in(true);

  /**
   * Render this operator and value-placeholder to StringBuilder
   * <p> sql snippet must start with a space
   *
   * @param sql SQL appender
   * @param value parameter to test
   * @param valueLength parameter length
   */
  void render(StringBuilder sql, @Nullable Object value, int valueLength);

  // Static Factory Methods

  static Operator plain(String placeholder) {
    return new Plain(placeholder);
  }

  static Operator in(boolean notIn) {
    return new In(notIn);
  }

  /**
   * column_name operator value;
   */
  record Plain(String placeholder) implements Operator {

    @Override
    public void render(StringBuilder sql, Object value, int valueLength) {
      sql.append(placeholder);
    }

  }

  class In implements Operator {

    private final boolean notIn;

    public In(boolean notIn) {
      this.notIn = notIn;
    }

    @Override
    public void render(StringBuilder sql, @Nullable Object value, int valueLength) {
      if (notIn) {
        sql.append(" NOT");
      }
      sql.append(" IN (");
      for (int i = 0; i < valueLength; i++) {
        if (i == 0) {
          sql.append('?');
        }
        else {
          sql.append(",?");
        }
      }
      sql.append(')');
    }

  }

}
