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
import java.time.OffsetTime;

/**
 * @author Tomas Rohovsky
 */
public class OffsetTimeTypeHandler extends BaseTypeHandler<OffsetTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, OffsetTime parameter) throws SQLException {
    ps.setTime(i, Time.valueOf(parameter.toLocalTime()));
  }

  @Override
  public OffsetTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getOffsetTime(rs.getTime(columnName));
  }

  @Override
  public OffsetTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getOffsetTime(rs.getTime(columnIndex));
  }

  @Override
  public OffsetTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getOffsetTime(cs.getTime(columnIndex));
  }

  static OffsetTime getOffsetTime(Time time) {
    if (time != null) {
      return time.toLocalTime().atOffset(OffsetTime.now().getOffset());
    }
    return null;
  }
}
