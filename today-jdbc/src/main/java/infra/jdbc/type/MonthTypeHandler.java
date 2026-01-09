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
import java.time.Month;

/**
 * @author Björn Raupach
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MonthTypeHandler extends BasicTypeHandler<Month> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Month arg) throws SQLException {
    ps.setInt(i, arg.getValue());
  }

  @Nullable
  @Override
  public Month getResult(ResultSet rs, String columnName) throws SQLException {
    int month = rs.getInt(columnName);
    return month == 0 && rs.wasNull() ? null : Month.of(month);
  }

  @Nullable
  @Override
  public Month getResult(ResultSet rs, int columnIndex) throws SQLException {
    int month = rs.getInt(columnIndex);
    return month == 0 && rs.wasNull() ? null : Month.of(month);
  }

  @Nullable
  @Override
  public Month getResult(CallableStatement cs, int columnIndex) throws SQLException {
    int month = cs.getInt(columnIndex);
    return month == 0 && cs.wasNull() ? null : Month.of(month);
  }

}
