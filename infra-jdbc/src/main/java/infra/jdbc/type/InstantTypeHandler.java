/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * A {@link TypeHandler} for {@link Instant}, binding it to JDBC timestamp values.
 *
 * <p>Reading uses {@link ResultSet#getObject(int, Class)} with {@link OffsetDateTime}
 * as the target type, so the value is interpreted using the connection time zone
 * rather than the JVM default time zone. This requires the JDBC driver to support
 * JDBC 4.2 target types; for MySQL a connection time zone such as
 * {@code serverTimezone=UTC} (or {@code connectionTimeZone=UTC} in Connector/J 8+)
 * should be configured explicitly.
 *
 * <p>Note that {@link Instant} has nanosecond precision while JDBC timestamp columns
 * usually retain only milliseconds or microseconds, so a value read back may be
 * truncated to the column precision. This is especially relevant when an
 * {@link Instant} is used as the version property of optimistic locking.
 *
 * @author Tomas Rohovsky
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OffsetDateTime
 * @since 4.0
 */
public class InstantTypeHandler extends BasicTypeHandler<Instant> {

  @Nullable
  @Override
  public Instant getResult(ResultSet rs, String columnName) throws SQLException {
    return getInstant(rs.getObject(columnName, OffsetDateTime.class));
  }

  @Nullable
  @Override
  public Instant getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getInstant(rs.getObject(columnIndex, OffsetDateTime.class));
  }

  @Nullable
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
