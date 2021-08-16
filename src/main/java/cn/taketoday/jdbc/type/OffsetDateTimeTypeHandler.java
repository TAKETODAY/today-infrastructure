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
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * @author Tomas Rohovsky
 */
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter) throws SQLException {
    ps.setTimestamp(i, Timestamp.from(parameter.toInstant()));
  }

  @Override
  public OffsetDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getOffsetDateTime(rs.getTimestamp(columnName));
  }

  @Override
  public OffsetDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getOffsetDateTime(rs.getTimestamp(columnIndex));
  }

  @Override
  public OffsetDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getOffsetDateTime(cs.getTimestamp(columnIndex));
  }

  static OffsetDateTime getOffsetDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
