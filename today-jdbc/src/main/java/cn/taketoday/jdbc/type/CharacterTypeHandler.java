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

package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CharacterTypeHandler extends BaseTypeHandler<Character> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Character parameter) throws SQLException {
    ps.setString(i, parameter.toString());
  }

  @Override
  public Character getResult(ResultSet rs, String columnName) throws SQLException {
    return getCharacter(rs.getString(columnName));
  }

  @Override
  public Character getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getCharacter(rs.getString(columnIndex));
  }

  @Override
  public Character getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getCharacter(cs.getString(columnIndex));
  }

  @Nullable
  protected Character getCharacter(@Nullable String columnValue) {
    return columnValue != null ? columnValue.charAt(0) : null;
  }

}
