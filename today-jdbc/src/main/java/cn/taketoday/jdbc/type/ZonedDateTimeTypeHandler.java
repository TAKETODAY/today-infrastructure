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
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import cn.taketoday.lang.Nullable;

/**
 * @author Tomas Rohovsky
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ZonedDateTimeTypeHandler extends AbstractZoneIdTypeHandler<ZonedDateTime> {

  public ZonedDateTimeTypeHandler() {
    super();
  }

  public ZonedDateTimeTypeHandler(ZoneId zoneId) {
    super(zoneId);
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, ZonedDateTime parameter) throws SQLException {
    ps.setTimestamp(i, Timestamp.from(parameter.toInstant()));
  }

  @Override
  public ZonedDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getZonedDateTime(rs.getTimestamp(columnName));
  }

  @Override
  public ZonedDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getZonedDateTime(rs.getTimestamp(columnIndex));
  }

  @Override
  public ZonedDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getZonedDateTime(cs.getTimestamp(columnIndex));
  }

  @Nullable
  protected ZonedDateTime getZonedDateTime(@Nullable Timestamp timestamp) {
    if (timestamp != null) {
      return ZonedDateTime.ofInstant(timestamp.toInstant(), zoneId);
    }
    return null;
  }

}
