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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

/**
 * @author Tomas Rohovsky
 * @since 3.4.5
 */
public class LocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter) throws SQLException {
    ps.setTime(i, Time.valueOf(parameter));
  }

  @Override
  public LocalTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getLocalTime(rs.getTime(columnName));
  }

  @Override
  public LocalTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getLocalTime(rs.getTime(columnIndex));
  }

  @Override
  public LocalTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getLocalTime(cs.getTime(columnIndex));
  }

  static LocalTime getLocalTime(Time time) {
    if (time != null) {
      return time.toLocalTime();
    }
    return null;
  }
}
