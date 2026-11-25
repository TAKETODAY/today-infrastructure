/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.util.DataSize;

/**
 * A {@link DataSize} type handler that stores the total number of bytes in a
 * database {@code BIGINT} column.
 *
 * <p>Both reading and writing go through {@link DataSize#toBytes()} and
 * {@link DataSize#ofBytes(long)}, so the stored value is always an exact byte
 * count. A SQL {@code NULL} is mapped to {@code null}, and a stored zero is
 * mapped to {@link DataSize#ofBytes(long) DataSize.ofBytes(0)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DataSizeTypeHandler extends BasicTypeHandler<DataSize> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, DataSize arg) throws SQLException {
    ps.setLong(parameterIndex, arg.toBytes());
  }

  @Override
  public @Nullable DataSize getResult(ResultSet rs, String columnName) throws SQLException {
    long bytes = rs.getLong(columnName);
    return rs.wasNull() ? null : DataSize.ofBytes(bytes);
  }

  @Override
  public @Nullable DataSize getResult(ResultSet rs, int columnIndex) throws SQLException {
    long bytes = rs.getLong(columnIndex);
    return rs.wasNull() ? null : DataSize.ofBytes(bytes);
  }

  @Override
  public @Nullable DataSize getResult(CallableStatement cs, int columnIndex) throws SQLException {
    long bytes = cs.getLong(columnIndex);
    return cs.wasNull() ? null : DataSize.ofBytes(bytes);
  }

}
