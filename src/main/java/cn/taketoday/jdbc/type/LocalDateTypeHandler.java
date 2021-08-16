/**
 * Copyright 2009-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * @since 3.4.5
 * @author Tomas Rohovsky
 */
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter) throws SQLException {
    ps.setDate(i, Date.valueOf(parameter));
  }

  @Override
  public LocalDate getResult(ResultSet rs, String columnName) throws SQLException {
    return getLocalDate(rs.getDate(columnName));
  }

  @Override
  public LocalDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getLocalDate(rs.getDate(columnIndex));
  }

  @Override
  public LocalDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getLocalDate(cs.getDate(columnIndex));
  }

  static LocalDate getLocalDate(Date date) {
    if (date != null) {
      return date.toLocalDate();
    }
    return null;
  }
}
