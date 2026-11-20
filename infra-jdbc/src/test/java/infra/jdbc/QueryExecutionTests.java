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

package infra.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.QueryTimeoutException;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.type.TypeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QueryExecutionTests {

  private static final String SQL = "select name from users";

  private final DataSource dataSource = mock();

  private final Connection connection = mock();

  private final PreparedStatement statement = mock();

  private final ResultSet rows = mock();

  private final RepositoryManager manager = new RepositoryManager(dataSource);

  @BeforeEach
  void configureConnection() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.prepareStatement(SQL)).willReturn(statement);
  }

  @Test
  void scalarReturnsNullAndClosesResourcesWhenNoRowsMatch() throws SQLException {
    given(statement.executeQuery()).willReturn(rows);

    assertThat(manager.createQuery(SQL).scalar(String.class)).isNull();

    verify(rows, never()).getString(1);
    verifyScalarCleanup();
  }

  @Test
  void scalarTranslatesSqlFailureAndStillClosesResources() throws SQLException {
    var timeout = new SQLTimeoutException("reading the first row timed out");
    given(statement.executeQuery()).willReturn(rows);
    given(rows.next()).willThrow(timeout);

    assertThatThrownBy(() -> manager.createQuery(SQL).scalar(String.class))
            .isInstanceOf(QueryTimeoutException.class).hasMessageContaining("Execute scalar").hasCause(timeout);

    verifyScalarCleanup();
  }

  @Test
  void scalarPreservesHandlerFailureAndStillClosesResources() throws SQLException {
    var failure = new IllegalStateException("invalid value");
    TypeHandler<String> handler = mock();
    given(statement.executeQuery()).willReturn(rows);
    given(rows.next()).willReturn(true);
    given(handler.getResult(rows, 1)).willThrow(failure);

    assertThatThrownBy(() -> manager.createQuery(SQL).scalar(handler)).isSameAs(failure);

    verifyScalarCleanup();
  }

  @Test
  void fetchTranslatesJdbcExecutionFailure() throws SQLException {
    var timeout = new SQLTimeoutException("executing the statement timed out");
    given(statement.executeQuery()).willThrow(timeout);

    try (JdbcConnection jdbc = manager.open()) {
      assertThatThrownBy(() -> jdbc.createQuery(SQL).fetch(String.class))
              .isInstanceOf(QueryTimeoutException.class).hasMessageContaining("Statement execute query").hasCause(timeout);
    }

    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void statementCallbackFailurePreventsExecution() throws SQLException {
    var timeout = new SQLTimeoutException("setting statement options timed out");

    try (JdbcConnection jdbc = manager.open()) {
      var query = jdbc.createNamedQuery(SQL).processStatement(ps -> {
        throw timeout;
      });
      assertThatThrownBy(query::executeUpdate)
              .isInstanceOf(QueryTimeoutException.class).hasMessageContaining("User statement callback").hasCause(timeout);
    }

    verify(statement, never()).executeUpdate();
    verify(statement).close();
  }

  @Test
  void bindsNamedTemporalValuesAndMapArgumentsInSqlOrder() throws SQLException {
    String sql = "insert into events (name, active, day, start_time) values (?, ?, ?, ?)";
    given(connection.prepareStatement(sql)).willReturn(statement);
    given(statement.executeUpdate()).willReturn(1);
    var day = LocalDate.of(2026, 11, 19);
    var start = LocalTime.of(10, 30);

    try (JdbcConnection jdbc = manager.open()) {
      var query = jdbc.createNamedQuery("insert into events (name, active, day, start_time) values (:name, :active, :day, :start)");
      assertThat(query.throwOnMappingFailure(true)).isSameAs(query);
      assertThat(query.toString()).isEqualTo(sql);
      query.addParameter("start", start).addParameter("day", day).addParameter("active", true)
              .addParameters(Map.of("name", "planning"));
      query.executeUpdate();
    }

    verify(statement).setString(1, "planning");
    verify(statement).setBoolean(2, true);
    verify(statement).setObject(3, day);
    verify(statement).setObject(4, start);
  }

  @Test
  void reportsWhichNamedParameterFailedToBindWithoutExecutingStatement() throws SQLException {
    String sql = "update users set name = ?";
    given(connection.prepareStatement(sql)).willReturn(statement);
    var failure = new SQLException("cannot encode name");
    willThrow(failure).given(statement).setString(1, "Ada");

    try (JdbcConnection jdbc = manager.open()) {
      var query = jdbc.createNamedQuery("update users set name = :name").addParameter("name", "Ada");

      assertThatThrownBy(query::executeUpdate).isInstanceOf(ParameterBindFailedException.class)
              .hasMessageContaining("'name'", "cannot encode name").hasCause(failure);
    }

    verify(statement, never()).executeUpdate();
    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void fetchFactoryReceivesMetadataAndMapsEachRow() throws SQLException {
    var metadata = mock(ResultSetMetaData.class);
    given(statement.executeQuery()).willReturn(rows);
    given(rows.getMetaData()).willReturn(metadata);
    given(rows.next()).willReturn(true, true, false);
    given(rows.getString(1)).willReturn("Ada", "Grace");
    ResultSetHandlerFactory<String> factory = columns -> {
      assertThat(columns).isSameAs(metadata);
      return row -> row.getString(1);
    };

    assertThat(manager.createQuery(SQL).fetch(factory)).containsExactly("Ada", "Grace");

    verify(rows).close();
  }

  @Test
  void fetchFirstExtractorClosesResultSetWithoutReadingTheNextRow() throws SQLException {
    given(statement.executeQuery()).willReturn(rows);
    given(rows.next()).willReturn(true);
    given(rows.getString(1)).willReturn("Ada");
    ResultSetExtractor<String> extractor = row -> row.getString(1);

    assertThat(manager.createQuery(SQL).fetchFirst(extractor)).isEqualTo("Ada");

    verify(rows).next();
    verify(rows).close();
  }

  @Test
  void translatesWhenDriverFailsToQueueBatchEntry() throws SQLException {
    var timeout = new SQLTimeoutException("queuing the batch timed out");
    willThrow(timeout).given(statement).addBatch();

    try (JdbcConnection jdbc = manager.open()) {
      assertThatThrownBy(() -> jdbc.createQuery(SQL).addToBatch())
              .isInstanceOf(QueryTimeoutException.class).hasMessageContaining("Adding statement to batch").hasCause(timeout);
    }

    verify(statement, never()).executeBatch();
  }

  @Test
  void translatesBatchExecutionFailureAndClosesStatement() throws SQLException {
    var timeout = new SQLTimeoutException("executing the batch timed out");
    given(statement.executeBatch()).willThrow(timeout);

    assertThatThrownBy(() -> manager.createQuery(SQL).addToBatch().executeBatch())
            .isInstanceOf(QueryTimeoutException.class).hasMessageContaining("Executing batch operation").hasCause(timeout);

    verify(statement).close();
    verify(connection).close();
  }

  @Test
  void preservesGeneratedKeyFailureAfterBatchExecution() throws SQLException {
    String insert = "insert into users(name) values ('Ada')";
    given(connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)).willReturn(statement);
    given(statement.executeBatch()).willReturn(new int[] { 1 });
    var failure = new SQLException("driver cannot return keys");
    given(statement.getGeneratedKeys()).willThrow(failure);

    assertThatThrownBy(() -> manager.createQuery(insert, true).addToBatch().executeBatch())
            .isInstanceOf(GeneratedKeysException.class).hasCause(failure);

    var order = inOrder(statement);
    order.verify(statement).executeBatch();
    order.verify(statement).getGeneratedKeys();
    order.verify(statement).close();
  }

  private void verifyScalarCleanup() throws SQLException {
    var order = inOrder(rows, statement, connection);
    order.verify(rows).close();
    order.verify(statement).close();
    order.verify(connection).close();
  }

}
