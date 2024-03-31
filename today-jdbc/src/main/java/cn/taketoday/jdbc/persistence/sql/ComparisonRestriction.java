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

/**
 * A binary-comparison restriction
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ComparisonRestriction implements Restriction {

  private final String lhs;

  private final String operator;

  private final String rhs;

  public ComparisonRestriction(String lhs, String operator, String rhs) {
    this.lhs = lhs;
    this.operator = operator;
    this.rhs = rhs;
  }

  @Override
  public void render(StringBuilder sqlBuffer) {
    sqlBuffer.append('`')
            .append(lhs)
            .append('`')
            .append(operator)
            .append(rhs);
  }

}
