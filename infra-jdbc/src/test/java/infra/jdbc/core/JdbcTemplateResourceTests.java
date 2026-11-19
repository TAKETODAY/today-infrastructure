/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.jdbc.BadSqlGrammarException;
import infra.jdbc.InvalidResultSetAccessException;
import infra.jdbc.SQLWarningException;
import infra.jdbc.UncategorizedSQLException;
import infra.jdbc.datasource.ConnectionProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JdbcTemplateResourceTests {

  private final DataSource dataSource = mock(DataSource.class);

  private final Connection connection = mock(Connection.class);

  private final JdbcTemplate template = new JdbcTemplate(dataSource);

  @BeforeEach
  void connect() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("statementFactories")
  void appliesTemplateSettingsToStatementsCreatedInConnectionCallback(String name,
          StatementFactory factory, Class<? extends Statement> statementType) throws SQLException {
    var statement = mock(statementType);
    given(factory.create(connection)).willReturn(statement);
    template.setFetchSize(12);
    template.setMaxRows(30);
    template.setQueryTimeout(5);

    template.execute((ConnectionCallback<Void>) con -> {
      assertThat(con.isClosed()).isFalse();
      assertThat(con.isWrapperFor(ConnectionProxy.class)).isTrue();
      assertThat(con.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(connection);
      try (var created = factory.create(con)) {
        assertThat(created).isSameAs(statement);
        verify(statement).setFetchSize(12);
        verify(statement).setMaxRows(30);
        verify(statement).setQueryTimeout(5);
      }
      return null;
    });

    var order = inOrder(statement, connection);
    order.verify(statement).close();
    order.verify(connection).close();
  }

  static Stream<Arguments> statementFactories() {
    String sql = "SELECT id FROM users";
    String call = "{call load_users()}";
    int scroll = ResultSet.TYPE_SCROLL_INSENSITIVE;
    int readOnly = ResultSet.CONCUR_READ_ONLY;
    int hold = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    return Stream.of(
            factory("statement", Connection::createStatement, Statement.class),
            factory("scrollable statement", con -> con.createStatement(scroll, readOnly), Statement.class),
            factory("holdable statement", con -> con.createStatement(scroll, readOnly, hold), Statement.class),
            factory("prepared", con -> con.prepareStatement(sql), PreparedStatement.class),
            factory("generated keys", con -> con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), PreparedStatement.class),
            factory("generated column indexes", con -> con.prepareStatement(sql, new int[] { 1 }), PreparedStatement.class),
            factory("generated column names", con -> con.prepareStatement(sql, new String[] { "id" }), PreparedStatement.class),
            factory("scrollable prepared", con -> con.prepareStatement(sql, scroll, readOnly), PreparedStatement.class),
            factory("holdable prepared", con -> con.prepareStatement(sql, scroll, readOnly, hold), PreparedStatement.class),
            factory("callable", con -> con.prepareCall(call), CallableStatement.class),
            factory("scrollable callable", con -> con.prepareCall(call, scroll, readOnly), CallableStatement.class),
            factory("holdable callable", con -> con.prepareCall(call, scroll, readOnly, hold), CallableStatement.class));
  }

  private static Arguments factory(String name, StatementFactory factory, Class<? extends Statement> statementType) {
    return Arguments.of(name, factory, statementType);
  }

  @Test
  void leavesPhysicalConnectionOpenWhenCallbackClosesProxyAndDelegatesUnwrapping() throws SQLException {
    var target = mock(VendorConnection.class);
    given(connection.isWrapperFor(VendorConnection.class)).willReturn(true);
    given(connection.unwrap(VendorConnection.class)).willReturn(target);

    var value = template.execute((ConnectionCallback<VendorConnection>) con -> {
      con.close();
      verify(connection, never()).close();
      assertThat(con.isWrapperFor(VendorConnection.class)).isTrue();
      return con.unwrap(VendorConnection.class);
    });

    assertThat(value).isSameAs(target);
    verify(connection).close();
  }

  @Test
  void parameterizedBatchFallsBackToIndividualUpdatesAndCleansUp() throws SQLException {
    String sql = "UPDATE users SET active = ?";
    var statement = batchStatement(sql, false);
    var setter = mock(DisposableBatchSetter.class);
    given(statement.executeUpdate()).willReturn(2, 1, 0);

    assertThat(template.batchUpdate(sql, List.of(true, false, true), 2, setter))
            .isDeepEqualTo(new int[][] { { 2 }, { 1 }, { 0 } });

    var order = inOrder(setter, statement, connection);
    order.verify(setter).setValues(statement, true);
    order.verify(statement).executeUpdate();
    order.verify(setter).setValues(statement, false);
    order.verify(statement).executeUpdate();
    order.verify(setter).setValues(statement, true);
    order.verify(statement).executeUpdate();
    order.verify(setter).cleanupParameters();
    order.verify(statement).close();
    order.verify(connection).close();
    verify(statement, never()).addBatch();
  }

  @Test
  void failedParameterizedBatchPreservesEarlierCountsAndDriverDetails() throws SQLException {
    String sql = "UPDATE users SET active = ?";
    var statement = batchStatement(sql, true);
    var setter = mock(DisposableBatchSetter.class);
    var failure = new BatchUpdateException("failed", "ZZ999", 17, new int[] { Statement.EXECUTE_FAILED });
    var detail = new SQLException("driver detail");
    var suppressed = new SQLException("suppressed driver detail");
    failure.setNextException(detail);
    failure.addSuppressed(suppressed);
    given(statement.executeBatch()).willReturn(new int[] { 1, 2 }).willThrow(failure);

    assertThatThrownBy(() -> template.batchUpdate(sql, List.of(true, false, true), 2, setter))
            .isInstanceOf(UncategorizedSQLException.class)
            .cause().isInstanceOfSatisfying(AggregatedBatchUpdateException.class, aggregated -> {
              assertThat(aggregated.getSuccessfulUpdateCounts()).isDeepEqualTo(new int[][] { { 1, 2 } });
              assertThat(aggregated.getUpdateCounts()).containsExactly(Statement.EXECUTE_FAILED);
              assertThat((Throwable) aggregated.getOriginalException()).isSameAs(failure);
              assertThat((Throwable) aggregated.getNextException()).isSameAs(detail);
              assertThat(aggregated.getSuppressed()).containsExactly(suppressed);
            });

    verify(statement, times(3)).addBatch();
    verify(statement, times(2)).executeBatch();
    var order = inOrder(setter, statement, connection);
    order.verify(setter).cleanupParameters();
    order.verify(statement).close();
    order.verify(connection).close();
  }

  private PreparedStatement batchStatement(String sql, boolean supportsBatch) throws SQLException {
    var metadata = mock(DatabaseMetaData.class);
    var statement = mock(PreparedStatement.class);
    given(connection.getMetaData()).willReturn(metadata);
    given(metadata.supportsBatchUpdates()).willReturn(supportsBatch);
    given(connection.prepareStatement(sql)).willReturn(statement);
    given(statement.getConnection()).willReturn(connection);
    return statement;
  }

  @Test
  void preparedStreamBindsParametersAndOwnsResourcesUntilClosed() throws SQLException {
    var statement = mock(PreparedStatement.class);
    var rows = mock(ResultSet.class);
    var setter = mock(DisposableStatementSetter.class);
    String sql = "SELECT name FROM users WHERE active = ?";
    given(connection.prepareStatement(sql)).willReturn(statement);
    given(statement.getConnection()).willReturn(connection);
    given(statement.executeQuery()).willReturn(rows);
    given(rows.next()).willReturn(true, true, false);
    given(rows.getString(1)).willReturn("Ada", "Lin");

    try (var stream = template.queryForStream(sql, setter, (rs, index) -> index + ":" + rs.getString(1))) {
      verify(rows, never()).next();
      var cursor = stream.spliterator();
      assertThat(cursor.trySplit()).isNull();
      assertThat(cursor.estimateSize()).isEqualTo(Long.MAX_VALUE);
      assertThat(cursor.hasCharacteristics(Spliterator.ORDERED)).isTrue();
      var names = new ArrayList<String>();
      cursor.forEachRemaining(names::add);
      assertThat(names).containsExactly("0:Ada", "1:Lin");
      verify(connection, never()).close();
    }

    var order = inOrder(setter, statement, rows, connection);
    order.verify(setter).setValues(statement);
    order.verify(statement).executeQuery();
    order.verify(rows).close();
    order.verify(setter).cleanupParameters();
    order.verify(statement).close();
    order.verify(connection).close();
  }

  @Test
  void streamWrapsReadFailureAndStillReleasesResourcesOnClose() throws SQLException {
    var statement = mock(Statement.class);
    var rows = mock(ResultSet.class);
    var failure = new SQLException("cursor lost");
    given(connection.createStatement()).willReturn(statement);
    given(statement.getConnection()).willReturn(connection);
    given(statement.executeQuery("SELECT name FROM users")).willReturn(rows);
    given(rows.next()).willThrow(failure);

    try (var stream = template.queryForStream("SELECT name FROM users", (rs, row) -> rs.getString(1))) {
      assertThatThrownBy(stream::findFirst).isInstanceOf(InvalidResultSetAccessException.class).hasCause(failure);
    }

    var order = inOrder(rows, statement, connection);
    order.verify(rows).close();
    order.verify(statement).close();
    order.verify(connection).close();
  }

  @Test
  void closesConnectionBeforeTranslatingStreamCreationFailure() throws SQLException {
    var statement = mock(Statement.class);
    var failure = new SQLException("query failed");
    var closed = new ArrayList<String>();
    String sql = "SELECT name FROM users";
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery(sql)).willThrow(failure);
    willAnswer(invocation -> {
      closed.add("statement");
      return null;
    }).given(statement).close();
    willAnswer(invocation -> {
      closed.add("connection");
      return null;
    }).given(connection).close();
    template.setExceptionTranslator((task, failedSql, ex) -> {
      assertThat(closed).containsExactly("statement", "connection");
      assertThat(failedSql).isEqualTo(sql);
      return new UncategorizedSQLException(task, failedSql, ex);
    });

    assertThatThrownBy(() -> template.queryForStream(sql, (rs, row) -> rs.getString(1)))
            .isInstanceOf(DataAccessException.class).hasCause(failure);
  }

  @Test
  void callExtractsDeclaredUpdateCountCustomValueAndBothCursorKinds() throws SQLException {
    var statement = mock(CallableStatement.class);
    var direct = mock(ResultSet.class);
    var mapped = mock(ResultSet.class);
    var defaultMapped = mock(ResultSet.class);
    var columns = mock(ResultSetMetaData.class);
    given(statement.getUpdateCount()).willReturn(7, -1, -1);
    given(statement.getMoreResults()).willReturn(true, false);
    given(statement.getResultSet()).willReturn(direct);
    given(statement.getInt(2)).willReturn(42);
    given(statement.getObject(3)).willReturn(mapped);
    given(statement.getObject(4)).willReturn(defaultMapped);
    given(mapped.next()).willReturn(true, false);
    given(mapped.getString(1)).willReturn("payload");
    given(defaultMapped.next()).willReturn(true, false);
    given(defaultMapped.getMetaData()).willReturn(columns);
    given(columns.getColumnCount()).willReturn(1);
    given(columns.getColumnLabel(1)).willReturn("ID");
    given(defaultMapped.getObject(1)).willReturn(23);
    ResultSetExtractor<String> extractor = rs -> {
      assertThat(rs).isSameAs(direct);
      return "extracted";
    };
    var declared = List.of(new SqlReturnUpdateCount("changed"), new SqlReturnResultSet("rows", extractor),
            new SqlParameter("input", Types.INTEGER), new SqlOutParameter("custom", Types.INTEGER, "custom_type",
            (cs, index, type, name) -> {
              assertThat(index).isEqualTo(2);
              assertThat(type).isEqualTo(Types.INTEGER);
              assertThat(name).isEqualTo("custom_type");
              return cs.getInt(index);
            }), new SqlOutParameter("mapped", Types.REF_CURSOR, (rs, row) -> rs.getString(1)),
            new SqlOutParameter("defaultMapped", Types.REF_CURSOR));

    var result = template.call(con -> statement, declared);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("changed", 7, "rows", "extracted", "custom", 42,
            "mapped", List.of("payload"), "defaultMapped", List.of(Map.of("ID", 23))));
    verify(direct).close();
    verify(mapped).close();
    verify(defaultMapped).close();
    verify(statement).close();
    verify(connection).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void skipFlagsDoNotSuppressExplicitOutputParameters(boolean skipAll) throws SQLException {
    var statement = mock(CallableStatement.class);
    var rows = mock(ResultSet.class);
    given(statement.execute()).willReturn(true);
    given(statement.getUpdateCount()).willReturn(-1);
    given(statement.getResultSet()).willReturn(rows);
    given(rows.next()).willReturn(true, false);
    given(rows.getInt(1)).willReturn(3);
    given(statement.getObject(1)).willReturn(4);
    template.setSkipResultsProcessing(skipAll);
    template.setSkipUndeclaredResults(true);

    var result = template.call(con -> statement, List.of(
            new SqlReturnResultSet("rows", (rs, row) -> rs.getInt(1)), new SqlOutParameter("total", Types.INTEGER)));

    assertThat(result).containsEntry("total", 4);
    if (skipAll) {
      assertThat(result).doesNotContainKey("rows");
      verify(statement, never()).getResultSet();
    }
    else {
      assertThat(result).containsEntry("rows", List.of(3));
      verify(rows).close();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void handlesUndeclaredUpdateCountsAndCursorAccordingToSkipPolicy(boolean skipUndeclared) throws SQLException {
    var statement = mock(CallableStatement.class);
    var rows = mock(ResultSet.class);
    given(statement.getUpdateCount()).willReturn(7, -1, -1);
    given(statement.getMoreResults()).willReturn(true, false);
    given(statement.getResultSet()).willReturn(rows);
    template.setSkipUndeclaredResults(skipUndeclared);

    var result = template.call(con -> statement, List.of());

    if (skipUndeclared) {
      assertThat(result).isEmpty();
      verify(statement, never()).getResultSet();
    }
    else {
      assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("#update-count-1", 7, "#result-set-1", List.of()));
      verify(rows).close();
    }
    verify(statement, times(2)).getMoreResults();
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void callableSqlIsReportedWhenExecutionFails() throws SQLException {
    String call = "{call load_users()}";
    var statement = mock(CallableStatement.class);
    var failure = new SQLSyntaxErrorException("procedure not found");
    given(connection.prepareCall(call)).willReturn(statement);
    given(statement.execute()).willThrow(failure);

    assertThatThrownBy(() -> template.execute(call, (CallableStatementCallback<Boolean>) CallableStatement::execute))
            .isInstanceOfSatisfying(BadSqlGrammarException.class, exception -> {
              assertThat(exception.getSql()).isEqualTo(call);
              assertThat(exception.getCause()).isSameAs(failure);
            });

    var order = inOrder(statement, connection);
    order.verify(statement).close();
    order.verify(connection).close();
  }

  @Test
  void raisesNonIgnoredWarningAfterSuccessfulExecution() throws SQLException {
    var statement = mock(Statement.class);
    var warning = new SQLWarning("driver warning");
    given(connection.createStatement()).willReturn(statement);
    given(statement.getWarnings()).willReturn(warning);
    template.setIgnoreWarnings(false);

    assertThatThrownBy(() -> template.execute((StatementCallback<Integer>) stmt -> 1))
            .isInstanceOfSatisfying(SQLWarningException.class,
                    exception -> assertThat((Throwable) exception.getSQLWarning()).isSameAs(warning));
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void chainsWarningToFailedExecutionWithoutReplacingSqlException() throws SQLException {
    String sql = "select name from users";
    var statement = mock(Statement.class);
    var failure = new SQLSyntaxErrorException("invalid column");
    var warning = new SQLWarning("driver warning");
    given(connection.createStatement()).willReturn(statement);
    given(statement.execute(sql)).willThrow(failure);
    given(statement.getWarnings()).willReturn(warning);
    template.setIgnoreWarnings(false);

    assertThatThrownBy(() -> template.execute(sql)).isInstanceOfSatisfying(BadSqlGrammarException.class, exception -> {
      assertThat(exception.getSql()).isEqualTo(sql);
      assertThat(exception.getCause()).isSameAs(failure);
      assertThat((Throwable) failure.getNextException()).isSameAs(warning);
    });
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void releasesConnectionBeforeTranslatingConnectionCallbackException() throws SQLException {
    var action = mock(SqlConnectionCallback.class);
    var failure = new SQLSyntaxErrorException("invalid query");
    String sql = "select id from users";
    given(action.getSql()).willReturn(sql);
    given(action.doInConnection(any(Connection.class))).willThrow(failure);
    var events = new ArrayList<String>();
    willAnswer(invocation -> {
      events.add("close");
      return null;
    }).given(connection).close();
    template.setExceptionTranslator((task, failedSql, ex) -> {
      assertThat(events).containsExactly("close");
      assertThat(task).isEqualTo("ConnectionCallback");
      assertThat(failedSql).isEqualTo(sql);
      return new BadSqlGrammarException(task, failedSql, ex);
    });

    assertThatThrownBy(() -> template.execute(action)).isInstanceOf(BadSqlGrammarException.class).hasCause(failure);

    verify(connection).close();
  }

  @Test
  void cleansUpPreparedParametersWhenExecutionFails() throws SQLException {
    var creator = mock(DisposableStatementCreator.class);
    var statement = mock(PreparedStatement.class);
    var failure = new SQLSyntaxErrorException("invalid query");
    given(creator.createPreparedStatement(connection)).willReturn(statement);
    given(statement.executeUpdate()).willThrow(failure);
    template.setIgnoreWarnings(false);
    given(statement.getWarnings()).willThrow(new SQLException("cannot read warnings"));

    assertThatThrownBy(() -> template.execute(creator, PreparedStatement::executeUpdate))
            .isInstanceOf(BadSqlGrammarException.class).hasCause(failure);

    var order = inOrder(creator, statement, connection);
    order.verify(statement).executeUpdate();
    order.verify(creator).cleanupParameters();
    order.verify(statement).getWarnings();
    order.verify(statement).close();
    order.verify(connection).close();
  }

  @Test
  void preservesStatementFailureWhenWarningProcessingThrowsUnexpectedException() throws SQLException {
    var statement = mock(Statement.class);
    var failure = new SQLSyntaxErrorException("invalid query");
    given(connection.createStatement()).willReturn(statement);
    given(statement.execute("select id from users")).willThrow(failure);
    given(statement.getWarnings()).willThrow(new IllegalStateException("cannot read warnings"));
    template.setIgnoreWarnings(false);

    assertThatThrownBy(() -> template.execute("select id from users"))
            .isInstanceOf(BadSqlGrammarException.class).hasCause(failure);

    verify(statement).close();
    verify(connection).close();
  }

  private interface SqlConnectionCallback extends ConnectionCallback<Object>, SqlProvider {
  }

  private interface DisposableStatementCreator extends PreparedStatementCreator, ParameterDisposer {
  }

  private interface DisposableStatementSetter extends PreparedStatementSetter, ParameterDisposer {
  }

  private interface DisposableBatchSetter extends ParameterizedPreparedStatementSetter<Boolean>, ParameterDisposer {
  }

  @FunctionalInterface
  private interface StatementFactory {

    Statement create(Connection connection) throws SQLException;
  }

  private interface VendorConnection extends Connection {
  }

}
