/*
 * Copyright 2012 - 2023 the original author or authors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import cn.taketoday.lang.Nullable;

/**
 * Duration Type handler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/11 14:23
 */
public class DurationTypeHandler extends BaseTypeHandler<Duration> {

  @Override
  public void setNonNullParameter(PreparedStatement ps,
          int parameterIndex, Duration parameter) throws SQLException {
    ps.setLong(parameterIndex, parameter.toNanos());
  }

  @Nullable
  @Override
  public Duration getResult(ResultSet rs, String columnName) throws SQLException {
    long nanos = rs.getLong(columnName);
    if (nanos == 0) {
      if (rs.wasNull()) {
        return null;
      }
      return Duration.ZERO;
    }
    return Duration.ofNanos(nanos);
  }

  @Nullable
  @Override
  public Duration getResult(ResultSet rs, int columnIndex) throws SQLException {
    long nanos = rs.getLong(columnIndex);
    if (nanos == 0) {
      if (rs.wasNull()) {
        return null;
      }
      return Duration.ZERO;
    }
    return Duration.ofNanos(nanos);
  }

  @Nullable
  @Override
  public Duration getResult(CallableStatement cs, int columnIndex) throws SQLException {
    long nanos = cs.getLong(columnIndex);
    if (nanos == 0) {
      if (cs.wasNull()) {
        return null;
      }
      return Duration.ZERO;
    }
    return Duration.ofNanos(nanos);
  }

}
