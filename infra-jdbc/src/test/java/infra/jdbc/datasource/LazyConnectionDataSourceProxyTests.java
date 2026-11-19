/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.util.ReflectionUtils;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Chengang Guan
 * @since 4.0 2023/8/9 14:09
 */
class LazyConnectionDataSourceProxyTests {

  private final LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();

  private final LazyConnectionDataSourceProxy lazyProxy = new LazyConnectionDataSourceProxy();

  @BeforeEach
  void setup() {
    lazyProxy.setDefaultAutoCommit(false);
    lazyProxy.setDefaultTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
  }

  @Test
  void setDefaultTransactionIsolationNameToUnsupportedValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(null));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("   "));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("bogus"));
  }

  /**
   * Verify that the internal 'constants' map is properly configured for all
   * TRANSACTION_ constants defined in {@link java.sql.Connection}.
   */
  @Test
  void setDefaultTransactionIsolationNameToAllSupportedValues() {
    Set<Integer> uniqueValues = new HashSet<>();
    streamIsolationConstants()
        .forEach(name -> {
          if ("TRANSACTION_NONE".equals(name)) {
            assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(name));
          }
          else {
            proxy.setDefaultTransactionIsolationName(name);
            Integer defaultTransactionIsolation = proxy.defaultTransactionIsolation();
            Integer expected = LazyConnectionDataSourceProxy.constants.get(name);
            assertThat(defaultTransactionIsolation).isEqualTo(expected);
            uniqueValues.add(defaultTransactionIsolation);
          }
        });
    assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(LazyConnectionDataSourceProxy.constants.values());
  }

  @Test
  void setDefaultTransactionIsolation() {
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(-999));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(TRANSACTION_NONE));

    proxy.setDefaultTransactionIsolation(TRANSACTION_READ_COMMITTED);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);

    proxy.setDefaultTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_UNCOMMITTED);

    proxy.setDefaultTransactionIsolation(TRANSACTION_REPEATABLE_READ);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_REPEATABLE_READ);

    proxy.setDefaultTransactionIsolation(TRANSACTION_SERIALIZABLE);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
  }

  @Test
  void credentialsAndReadOnlyFlagSelectPhysicalDataSourceLazily() throws SQLException {
    DataSource writable = mock();
    DataSource readOnly = mock();
    Connection primary = mock();
    Connection replica = mock();
    when(writable.getConnection("user", "secret")).thenReturn(primary);
    when(readOnly.getConnection("reader", "secret")).thenReturn(replica);
    lazyProxy.setTargetDataSource(writable);
    lazyProxy.setReadOnlyDataSource(readOnly);

    try (var reader = lazyProxy.getConnection("reader", "secret");
            var writer = lazyProxy.getConnection("user", "secret")) {
      reader.setReadOnly(true);
      assertThat(reader.isReadOnly()).isTrue();
      assertThat(writer.isReadOnly()).isFalse();
      assertThat(reader.isWrapperFor(ConnectionProxy.class)).isTrue();
      assertThat(reader.unwrap(ConnectionProxy.class)).isSameAs(reader);
      assertThat(reader).isNotEqualTo(writer);
      assertThat(reader.hashCode()).isEqualTo(System.identityHashCode(reader));
      verify(readOnly, never()).getConnection("reader", "secret");
      verify(writable, never()).getConnection("user", "secret");

      assertThat(reader.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(replica);
      assertThat(writer.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(primary);
      reader.setReadOnly(false);
      assertThat(reader.hashCode()).isEqualTo(System.identityHashCode(reader));
      verify(replica, never()).setReadOnly(true);
      verify(replica, never()).setReadOnly(false);
    }
    verify(replica).close();
    verify(primary).close();
  }

  @Test
  void readOnlyDataSourceCanBeUsedAsSoleTarget() throws SQLException {
    DataSource readOnly = mock();
    Connection physical = mock();
    when(readOnly.getConnection()).thenReturn(physical);
    lazyProxy.setReadOnlyDataSource(readOnly);

    try (var connection = lazyProxy.getConnection()) {
      assertThat(connection.isReadOnly()).isTrue();
      assertThat(connection.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(physical);
    }
    verify(physical).close();
  }

  @Test
  void closedUninitializedHandleRejectsStatementCreationWithoutBorrowing() throws SQLException {
    DataSource dataSource = mock();
    lazyProxy.setTargetDataSource(dataSource);

    var connection = lazyProxy.getConnection();
    assertThat(connection.isClosed()).isFalse();
    assertThat(connection.toString()).contains("Lazy Connection proxy");
    connection.commit();
    connection.rollback();
    connection.close();

    assertThat(connection.isClosed()).isTrue();
    assertThatExceptionOfType(SQLException.class).isThrownBy(() -> establishPhysicalConnection(connection))
            .withMessageContaining("connection is closed");
    verify(dataSource, never()).getConnection();
  }

  @Test
  void failedSettingsLeaveHandleAvailableToRetryWithFreshPhysicalConnection() throws SQLException {
    DataSource dataSource = mock();
    Connection first = mock();
    Connection second = mock();
    var failure = new SQLException("schema rejected");
    when(dataSource.getConnection()).thenReturn(first, second);
    doThrow(failure).when(first).setSchema("app");
    doThrow(new SQLException("close failed")).when(first).close();
    lazyProxy.setTargetDataSource(dataSource);

    try (var connection = lazyProxy.getConnection()) {
      connection.setSchema("app");
      assertThatExceptionOfType(SQLException.class).isThrownBy(() -> establishPhysicalConnection(connection))
              .isSameAs(failure);
      verify(first).close();
      assertThat(connection.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(second);
      verify(second).setSchema("app");
    }
    verify(second).close();
  }

  @Test
  void suppressesRollbackOnClosedTargetButPropagatesDriverExceptionOtherwise() throws SQLException {
    DataSource dataSource = mock();
    Connection physical = mock();
    var failure = new SQLException("rollback failed");
    when(dataSource.getConnection()).thenReturn(physical);
    when(physical.isClosed()).thenReturn(true, false);
    doThrow(failure).when(physical).rollback();
    lazyProxy.setTargetDataSource(dataSource);

    try (var connection = lazyProxy.getConnection()) {
      establishPhysicalConnection(connection);
      connection.rollback();
      verify(physical, never()).rollback();
      assertThatExceptionOfType(SQLException.class).isThrownBy(connection::rollback).isSameAs(failure);
    }
  }

  @Test
  void retriesDefaultPropertyLookupAfterAcquisitionFailure() throws SQLException {
    DataSource dataSource = mock();
    Connection defaults = mock();
    when(dataSource.getConnection()).thenThrow(new SQLException("temporarily unavailable")).thenReturn(defaults);
    when(defaults.getTransactionIsolation()).thenReturn(TRANSACTION_READ_COMMITTED);
    proxy.setDefaultAutoCommit(true);
    proxy.setTargetDataSource(dataSource);

    proxy.checkDefaultConnectionProperties();
    try (var connection = proxy.getConnection()) {
      assertThat(connection.getAutoCommit()).isTrue();
      assertThat(connection.getTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);
    }

    verify(defaults, never()).getAutoCommit();
    verify(defaults).getTransactionIsolation();
    verify(defaults).close();
  }

  @Test
  void lazyHandlingOfCatalog() throws SQLException {
    DataSource dataSource = mock();
    Connection first = mock();
    Connection second = mock();
    when(dataSource.getConnection()).thenReturn(first, second);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setCatalog("catalogName");
    assertThat(lazyConnection.getCatalog()).isEqualTo("catalogName");
    verify(first, never()).setCatalog("catalogName");
    verify(first, never()).getCatalog();
    establishPhysicalConnection(lazyConnection);
    verify(first).setCatalog("catalogName");

    lazyProxy.getConnection().getCatalog();
    verify(second).getCatalog();
  }

  @Test
  void lazyHandlingOfSchema() throws SQLException {
    DataSource dataSource = mock();
    Connection first = mock();
    Connection second = mock();
    when(dataSource.getConnection()).thenReturn(first, second);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setSchema("schemaName");
    assertThat(lazyConnection.getSchema()).isEqualTo("schemaName");
    verify(first, never()).setSchema("schemaName");
    verify(first, never()).getSchema();
    establishPhysicalConnection(lazyConnection);
    verify(first).setSchema("schemaName");

    lazyProxy.getConnection().getSchema();
    verify(second).getSchema();
  }

  @Test
  void lazyHandlingOfHoldability() throws SQLException {
    DataSource dataSource = mock();
    Connection first = mock();
    Connection second = mock();
    when(dataSource.getConnection()).thenReturn(first, second);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setHoldability(CLOSE_CURSORS_AT_COMMIT);
    assertThat(lazyConnection.getHoldability()).isEqualTo(CLOSE_CURSORS_AT_COMMIT);
    verify(first, never()).setHoldability(CLOSE_CURSORS_AT_COMMIT);
    verify(first, never()).getHoldability();
    establishPhysicalConnection(lazyConnection);
    verify(first).setHoldability(CLOSE_CURSORS_AT_COMMIT);

    lazyProxy.getConnection().getHoldability();
    verify(second).getHoldability();
  }

  @Test
  void lazyHandlingOfTransactionIsolation() throws SQLException {
    DataSource dataSource = mock();
    Connection physicalConnection = mock();
    when(dataSource.getConnection()).thenReturn(physicalConnection);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
    assertThat(lazyConnection.getTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);
    verify(physicalConnection, never()).setTransactionIsolation(TRANSACTION_READ_COMMITTED);
    verify(physicalConnection, never()).getTransactionIsolation();
    establishPhysicalConnection(lazyConnection);
    verify(physicalConnection).setTransactionIsolation(TRANSACTION_READ_COMMITTED);
  }

  @Test
  void lazyHandlingOfAutoCommit() throws SQLException {
    DataSource dataSource = mock();
    Connection physicalConnection = mock();
    when(dataSource.getConnection()).thenReturn(physicalConnection);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setAutoCommit(true);
    assertThat(lazyConnection.getAutoCommit()).isTrue();
    verify(physicalConnection, never()).setAutoCommit(true);
    verify(physicalConnection, never()).getAutoCommit();
    establishPhysicalConnection(lazyConnection);
    verify(physicalConnection).setAutoCommit(true);
  }

  @Test
  void lazyHandlingOfNetworkTimeoutExecutor() throws SQLException {
    DataSource dataSource = mock();
    Connection first = mock();
    Connection second = mock();
    Executor executor = mock();
    when(dataSource.getConnection()).thenReturn(first, second);
    doThrow(SQLException.class).when(second).setNetworkTimeout(eq(null), anyInt());
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setNetworkTimeout(executor, 1000);
    assertThat(lazyConnection.getNetworkTimeout()).isEqualTo(1000);
    verify(first, never()).setNetworkTimeout(executor, 1000);
    verify(first, never()).getNetworkTimeout();
    establishPhysicalConnection(lazyConnection);
    verify(first).setNetworkTimeout(executor, 1000);

    Connection secondLazyConnection = lazyProxy.getConnection();
    secondLazyConnection.setNetworkTimeout(null, 1000);
    assertThatExceptionOfType(SQLException.class).isThrownBy(() -> establishPhysicalConnection(secondLazyConnection));
  }

  @Test
  void lazyHandlingOfClientInfoForKeyValuePairs() throws SQLException {
    DataSource dataSource = mock();
    Connection physicalConnection = mock();
    when(dataSource.getConnection()).thenReturn(physicalConnection);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    lazyConnection.setClientInfo("k1", "v1");
    lazyConnection.setClientInfo("k2", "v2");
    verify(physicalConnection, never()).setClientInfo("k1", "v1");
    verify(physicalConnection, never()).setClientInfo("k2", "v2");
    lazyConnection.getClientInfo("k1");
    verify(physicalConnection).setClientInfo("k1", "v1");
    verify(physicalConnection).setClientInfo("k2", "v2");
    verify(physicalConnection).getClientInfo("k1");
  }

  @Test
  void nonLazyHandlingOfClientInfoForProperties() throws SQLException {
    DataSource dataSource = mock();
    Connection physicalConnection = mock();
    when(dataSource.getConnection()).thenReturn(physicalConnection);
    lazyProxy.setTargetDataSource(dataSource);

    Connection lazyConnection = lazyProxy.getConnection();
    Properties properties = new Properties();
    properties.setProperty("k1", "v1");
    properties.setProperty("k2", "v2");
    lazyConnection.setClientInfo(properties);
    verify(physicalConnection).setClientInfo(properties);
  }

  private static void establishPhysicalConnection(Connection lazyConnection) throws SQLException {
    lazyConnection.prepareStatement("SELECT 1");
  }

  private static Stream<String> streamIsolationConstants() {
    return Arrays.stream(Connection.class.getFields())
        .filter(ReflectionUtils::isPublicStaticFinal)
        .map(Field::getName)
        .filter(name -> name.startsWith("TRANSACTION_"));
  }

}
