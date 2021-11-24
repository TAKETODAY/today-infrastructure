/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;

/**
 * Type Handler for {@link JapaneseDate}.
 *
 * @author Kazuki Shimizu
 */
public class JapaneseDateTypeHandler extends BaseTypeHandler<JapaneseDate> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, JapaneseDate parameter) throws SQLException {
    ps.setDate(i, Date.valueOf(LocalDate.ofEpochDay(parameter.toEpochDay())));
  }

  @Override
  public JapaneseDate getResult(ResultSet rs, String columnName) throws SQLException {
    return getJapaneseDate(rs.getDate(columnName));
  }

  @Override
  public JapaneseDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getJapaneseDate(rs.getDate(columnIndex));
  }

  @Override
  public JapaneseDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getJapaneseDate(cs.getDate(columnIndex));
  }

  private static JapaneseDate getJapaneseDate(Date date) {
    if (date != null) {
      return JapaneseDate.from(date.toLocalDate());
    }
    return null;
  }

}
