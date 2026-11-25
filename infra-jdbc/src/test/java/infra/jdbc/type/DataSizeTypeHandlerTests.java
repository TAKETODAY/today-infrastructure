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

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.util.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DataSizeTypeHandlerTests {

  private static final DataSize SAMPLE = DataSize.ofMegabytes(5);

  private final DataSizeTypeHandler handler = new DataSizeTypeHandler();

  @Test
  void registeredAsDefaultHandler() {
    assertThat(new TypeHandlerManager().getTypeHandler(DataSize.class))
            .isInstanceOf(DataSizeTypeHandler.class);
  }

  @Test
  void setsTotalBytes() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    handler.setParameter(statement, 1, SAMPLE);

    verify(statement).setLong(1, SAMPLE.toBytes());
  }

  @Test
  void setsZeroAndNegativeBytes() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    handler.setParameter(statement, 1, DataSize.ofBytes(0));
    handler.setParameter(statement, 2, DataSize.ofBytes(-1024));

    verify(statement).setLong(1, 0L);
    verify(statement).setLong(2, -1024L);
  }

  @Test
  void setsNullParameter() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    handler.setParameter(statement, 1, null);

    verify(statement).setObject(1, null);
  }

  @Test
  void getsResultByColumnName() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong("size_col")).willReturn(SAMPLE.toBytes());
    given(resultSet.wasNull()).willReturn(false);

    assertThat(handler.getResult(resultSet, "size_col")).isEqualTo(SAMPLE);
  }

  @Test
  void getsZeroBytesByColumnName() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong("size_col")).willReturn(0L);
    given(resultSet.wasNull()).willReturn(false);

    assertThat(handler.getResult(resultSet, "size_col")).isEqualTo(DataSize.ofBytes(0));
  }

  @Test
  void getsNullByColumnName() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong("size_col")).willReturn(0L);
    given(resultSet.wasNull()).willReturn(true);

    assertThat(handler.getResult(resultSet, "size_col")).isNull();
  }

  @Test
  void getsResultByColumnIndex() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong(3)).willReturn(SAMPLE.toBytes());
    given(resultSet.wasNull()).willReturn(false);

    assertThat(handler.getResult(resultSet, 3)).isEqualTo(SAMPLE);
  }

  @Test
  void getsNullByColumnIndex() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong(3)).willReturn(0L);
    given(resultSet.wasNull()).willReturn(true);

    assertThat(handler.getResult(resultSet, 3)).isNull();
  }

  @Test
  void getsResultFromCallableStatement() throws SQLException {
    CallableStatement statement = mock(CallableStatement.class);
    given(statement.getLong(1)).willReturn(SAMPLE.toBytes());
    given(statement.wasNull()).willReturn(false);

    assertThat(handler.getResult(statement, 1)).isEqualTo(SAMPLE);
  }

  @Test
  void getsNullFromCallableStatement() throws SQLException {
    CallableStatement statement = mock(CallableStatement.class);
    given(statement.getLong(1)).willReturn(0L);
    given(statement.wasNull()).willReturn(true);

    assertThat(handler.getResult(statement, 1)).isNull();
  }

}
