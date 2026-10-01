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
