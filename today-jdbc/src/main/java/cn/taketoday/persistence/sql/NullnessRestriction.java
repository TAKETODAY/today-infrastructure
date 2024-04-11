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
 * Nullness restriction - IS (NOT)? NULL
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class NullnessRestriction implements Restriction {

  private final String columnName;

  private final boolean affirmative;

  NullnessRestriction(String columnName, boolean affirmative) {
    this.columnName = columnName;
    this.affirmative = affirmative;
  }

  @Override
  public void render(StringBuilder sqlBuffer) {
    sqlBuffer.append(columnName);
    if (affirmative) {
      sqlBuffer.append(" is null");
    }
    else {
      sqlBuffer.append(" is not null");
    }
  }
}
