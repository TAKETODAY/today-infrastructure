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

package infra.jdbc.format;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class LoggingPreparedStatementTests {

  @Test
  void collectsBoundParametersAndLogsUpdatesOnExecuteUpdate() throws Exception {
    PreparedStatement delegate = mock(PreparedStatement.class);
    given(delegate.executeUpdate()).willReturn(1);

    List<Object[]> captured = new ArrayList<>();
    List<String> outcomes = new ArrayList<>();
    PreparedStatement proxy = LoggingPreparedStatement.wrap(delegate, capturingLogger(captured, outcomes));

    proxy.setString(1, "TODAY");
    proxy.setInt(2, 10);
    proxy.executeUpdate();

    assertThat(captured).hasSize(1);
    assertThat(captured.get(0)).containsExactly("TODAY", 10);
    assertThat(outcomes).containsExactly("<== Updates: 1");
  }

  @Test
  void setNullIsLoggedAsNull() throws Exception {
    PreparedStatement delegate = mock(PreparedStatement.class);
    given(delegate.executeUpdate()).willReturn(1);

    List<Object[]> captured = new ArrayList<>();
    PreparedStatement proxy = LoggingPreparedStatement.wrap(delegate, capturingLogger(captured, new ArrayList<>()));

    proxy.setNull(1, Types.VARCHAR);
    proxy.executeUpdate();

    assertThat(captured.get(0)).containsExactly((Object) null);
  }

  @Test
  void clearParametersResetsCollectedParameters() throws Exception {
    PreparedStatement delegate = mock(PreparedStatement.class);
    given(delegate.executeUpdate()).willReturn(1);

    List<Object[]> captured = new ArrayList<>();
    PreparedStatement proxy = LoggingPreparedStatement.wrap(delegate, capturingLogger(captured, new ArrayList<>()));

    proxy.setString(1, "TODAY");
    proxy.clearParameters();
    proxy.executeUpdate();

    assertThat(captured.get(0)).isEmpty();
  }

  @Test
  void logsColumnsRowsAndTotalForResultSet() throws Exception {
    PreparedStatement delegate = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    ResultSetMetaData metaData = mock(ResultSetMetaData.class);

    given(delegate.executeQuery()).willReturn(resultSet);
    given(resultSet.getMetaData()).willReturn(metaData);
    given(metaData.getColumnCount()).willReturn(2);
    given(metaData.getColumnLabel(1)).willReturn("id");
    given(metaData.getColumnLabel(2)).willReturn("name");
    given(resultSet.next()).willReturn(true, true, false);
    given(resultSet.getObject(1)).willReturn(1, 2);
    given(resultSet.getObject(2)).willReturn("TODAY", "INFRA");

    List<String> outcomes = new ArrayList<>();
    PreparedStatement proxy = LoggingPreparedStatement.wrap(delegate, capturingLogger(new ArrayList<>(), outcomes));

    ResultSet result = proxy.executeQuery();
    while (result.next()) {
      // consume
    }
    result.close();

    assertThat(outcomes).containsExactly(
            "<==    Columns: id, name",
            "<==        Row: [1(Integer), TODAY(String)]",
            "<==        Row: [2(Integer), INFRA(String)]",
            "<== Total: 2");
  }

  @Test
  void nonExecutionIntegerResultIsNotLogged() throws Exception {
    PreparedStatement delegate = mock(PreparedStatement.class);
    given(delegate.getUpdateCount()).willReturn(5);

    List<String> outcomes = new ArrayList<>();
    PreparedStatement proxy = LoggingPreparedStatement.wrap(delegate, capturingLogger(new ArrayList<>(), outcomes));

    assertThat(proxy.getUpdateCount()).isEqualTo(5);
    assertThat(outcomes).isEmpty();
  }

  @Test
  void returnsOriginalStatementWhenLoggingDisabled() {
    PreparedStatement delegate = mock(PreparedStatement.class);
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    assertThat(LoggingPreparedStatement.wrap(delegate, logger)).isSameAs(delegate);
  }

  @Test
  void formatsParameterValuesWithType() {
    assertThat(logger().formatParameters(new Object[] { 10, "TODAY", null }))
            .isEqualTo("[10(Integer), TODAY(String), null]");
  }

  @Test
  void truncatesCharSequenceByConfiguredLength() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, false, 0, null, 3);
    assertThat(logger.formatParameters(new Object[] { "TODAY" })).isEqualTo("[TOD (truncated)...(String)]");
  }

  @Test
  void nonPositiveMaxParameterLengthDisablesTruncation() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, false, 0, null, 0);
    assertThat(logger.formatParameters(new Object[] { "TODAY" })).isEqualTo("[TODAY(String)]");
  }

  private static SqlStatementLogger logger() {
    return new SqlStatementLogger(false, false, false, 0);
  }

  private static SqlStatementLogger capturingLogger(List<Object[]> parameters, List<String> outcomes) {
    return new SqlStatementLogger(false, false, false, 0) {

      @Override
      public boolean isDebugEnabled() {
        return true;
      }

      @Override
      public void logParameters(Object[] values) {
        parameters.add(values);
      }

      @Override
      public void logOutcome(String text) {
        outcomes.add(text);
      }
    };
  }

}
