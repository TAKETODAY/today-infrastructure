/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.type;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;

import infra.lang.Nullable;

/**
 * Config server time zone like: {@code serverTimezone=UTC}
 *
 * @author Tomas Rohovsky
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InstantTypeHandler extends BasicTypeHandler<Instant> {

  @Override
  public Instant getResult(ResultSet rs, String columnName) throws SQLException {
    return getInstant(rs.getObject(columnName, OffsetDateTime.class));
  }

  @Override
  public Instant getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getInstant(rs.getObject(columnIndex, OffsetDateTime.class));
  }

  @Override
  public Instant getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getInstant(cs.getObject(columnIndex, OffsetDateTime.class));
  }

  @Nullable
  private static Instant getInstant(@Nullable OffsetDateTime timestamp) {
    if (timestamp != null) {
      return timestamp.toInstant();
    }
    return null;
  }

}
