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

package infra.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DelegatingConnectionTests {

  private final Connection delegate = mock(Connection.class);

  private final Connection connection = new DelegatingConnection(delegate);

  @Test
  void createsStatementsWithRequestedOptions() throws SQLException {
    int type = ResultSet.TYPE_SCROLL_INSENSITIVE;
    int concurrency = ResultSet.CONCUR_READ_ONLY;
    int holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    String query = "select id from users";
    String call = "{call refresh_users()}";
    String insert = "insert into users (name) values (?)";
    int[] indexes = { 1 };
    String[] names = { "id" };

    assertReturns(Connection::createStatement, mock(Statement.class));
    assertReturns(it -> it.createStatement(type, concurrency), mock(Statement.class));
    assertReturns(it -> it.createStatement(type, concurrency, holdability), mock(Statement.class));
    assertReturns(it -> it.prepareStatement(query), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareStatement(query, type, concurrency), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareStatement(query, type, concurrency, holdability), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareStatement(insert, indexes), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareStatement(insert, names), mock(PreparedStatement.class));
    assertReturns(it -> it.prepareCall(call), mock(CallableStatement.class));
    assertReturns(it -> it.prepareCall(call, type, concurrency), mock(CallableStatement.class));
    assertReturns(it -> it.prepareCall(call, type, concurrency, holdability), mock(CallableStatement.class));
    assertReturns(it -> it.nativeSQL(call), "CALL refresh_users()");
  }

  @Test
  void delegatesTransactionsAndSavepoints() throws SQLException {
    var savepoint = mock(Savepoint.class);
    connection.setAutoCommit(false);
    connection.commit();
    connection.rollback();
    connection.rollback(savepoint);
    connection.releaseSavepoint(savepoint);
    connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    verify(delegate).setAutoCommit(false);
    verify(delegate).commit();
    verify(delegate).rollback();
    verify(delegate).rollback(savepoint);
    verify(delegate).releaseSavepoint(savepoint);
    verify(delegate).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    assertReturns(Connection::getAutoCommit, true);
    assertReturns(Connection::getTransactionIsolation, Connection.TRANSACTION_SERIALIZABLE);
    assertReturns(Connection::setSavepoint, mock(Savepoint.class));
    assertReturns(it -> it.setSavepoint("checkpoint"), savepoint);
  }

  @Test
  void delegatesConnectionProperties() throws SQLException {
    Map<String, Class<?>> types = Map.of("CUSTOM", String.class);
    var clientInfo = new Properties();
    clientInfo.setProperty("ApplicationName", "example");
    Executor executor = Runnable::run;

    connection.setReadOnly(true);
    connection.setCatalog("catalog");
    connection.setSchema("schema");
    connection.setTypeMap(types);
    connection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
    connection.setClientInfo(clientInfo);
    connection.setClientInfo("ApplicationName", "example");
    connection.setNetworkTimeout(executor, 2000);
    connection.clearWarnings();
    verify(delegate).setReadOnly(true);
    verify(delegate).setCatalog("catalog");
    verify(delegate).setSchema("schema");
    verify(delegate).setTypeMap(types);
    verify(delegate).setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
    verify(delegate).setClientInfo(clientInfo);
    verify(delegate).setClientInfo("ApplicationName", "example");
    verify(delegate).setNetworkTimeout(executor, 2000);
    verify(delegate).clearWarnings();

    assertReturns(Connection::isReadOnly, true);
    assertReturns(Connection::getCatalog, "catalog");
    assertReturns(Connection::getSchema, "schema");
    assertReturns(Connection::getTypeMap, types);
    assertReturns(Connection::getHoldability, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    assertReturns(Connection::getClientInfo, clientInfo);
    assertReturns(it -> it.getClientInfo("ApplicationName"), "example");
    assertReturns(Connection::getNetworkTimeout, 2000);
    assertReturns(Connection::getWarnings, new SQLWarning("warning"));
    assertReturns(Connection::getMetaData, mock(DatabaseMetaData.class));
  }

  @Test
  void createsDriverSpecificValues() throws SQLException {
    Object[] elements = { 1, 2 };
    Object[] attributes = { "test" };
    assertReturns(Connection::createBlob, mock(Blob.class));
    assertReturns(Connection::createClob, mock(Clob.class));
    assertReturns(Connection::createNClob, mock(NClob.class));
    assertReturns(Connection::createSQLXML, mock(SQLXML.class));
    assertReturns(it -> it.createArrayOf("INTEGER", elements), mock(Array.class));
    assertReturns(it -> it.createStruct("CUSTOM", attributes), mock(Struct.class));
  }

  @Test
  void delegatesLifecycleAndWrapping() throws SQLException {
    Executor executor = Runnable::run;
    connection.beginRequest();
    connection.endRequest();
    connection.abort(executor);
    connection.close();
    verify(delegate).beginRequest();
    verify(delegate).endRequest();
    verify(delegate).abort(executor);
    verify(delegate).close();

    assertReturns(Connection::isClosed, true);
    assertReturns(it -> it.isValid(5), true);
    assertReturns(it -> it.isWrapperFor(Connection.class), true);
    assertReturns(it -> it.unwrap(Connection.class), delegate);

    SQLException failure = new SQLException("unavailable");
    given(delegate.createStatement()).willThrow(failure);
    assertThatThrownBy(connection::createStatement).isSameAs(failure);
  }

  @Test
  void forwardsBothShardingKeyOverloads() throws SQLException {
    var key = mock(ShardingKey.class);
    var superKey = mock(ShardingKey.class);
    connection.setShardingKey(key);
    connection.setShardingKey(key, superKey);
    verify(delegate).setShardingKey(key);
    verify(delegate).setShardingKey(key, superKey);

    assertReturns(it -> it.setShardingKeyIfValid(key, 5), true);
    assertReturns(it -> it.setShardingKeyIfValid(key, superKey, 5), true);
  }

  private <T> void assertReturns(ConnectionOperation<T> operation, T result) throws SQLException {
    given(operation.invoke(delegate)).willReturn(result);
    assertThat(operation.invoke(connection)).isEqualTo(result);
  }

  @FunctionalInterface
  private interface ConnectionOperation<T> {

    T invoke(Connection connection) throws SQLException;

  }

}
