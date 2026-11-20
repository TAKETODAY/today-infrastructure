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

package infra.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import infra.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TransactionAwareDataSourceProxyTests {

  private final DataSource source = mock(DataSource.class);

  private final Connection physical = mock(Connection.class);

  @Test
  void wrapperInspectionAndClosingUnusedHandleDoNotBorrowAConnection() throws SQLException {
    var proxy = new TransactionAwareDataSourceProxy();
    proxy.setTargetDataSource(source);
    try (var first = proxy.getConnection(); var second = proxy.getConnection()) {
      assertThat(first).isEqualTo(first).isNotEqualTo(second);
      assertThat(first.hashCode()).isEqualTo(System.identityHashCode(first));
      assertThat(first.toString()).contains("from DataSource");
      assertThat(first.isWrapperFor(ConnectionProxy.class)).isTrue();
      assertThat(first.unwrap(ConnectionProxy.class)).isSameAs(first);
      assertThat(first.isClosed()).isFalse();
      assertThat((Throwable) first.getWarnings()).isNull();
      first.clearWarnings();
      second.close();
      assertThat(second.isClosed()).isTrue();
      assertThatThrownBy(second::createStatement).isInstanceOf(SQLException.class).hasMessageContaining("already closed");
    }
    verifyNoInteractions(source);
  }

  @Test
  void eagerProxyDelegatesVendorUnwrapAndClosesUnboundConnection() throws SQLException {
    given(source.getConnection()).willReturn(physical);
    var vendor = mock(VendorConnection.class);
    given(physical.unwrap(VendorConnection.class)).willReturn(vendor);
    given(physical.isWrapperFor(VendorConnection.class)).willReturn(true);
    var proxy = new TransactionAwareDataSourceProxy(source);
    proxy.setLazyTransactionalConnections(false);
    proxy.setReobtainTransactionalConnections(true);

    try (var connection = proxy.getConnection()) {
      verify(source).getConnection();
      assertThat(connection.toString()).contains(physical.toString()).doesNotContain("from DataSource");
      assertThat(connection.isWrapperFor(VendorConnection.class)).isTrue();
      assertThat(connection.unwrap(VendorConnection.class)).isSameAs(vendor);
    }

    verify(physical).close();
  }

  @Test
  void appliesBoundTransactionTimeoutWithoutClosingItsPhysicalConnection() throws SQLException {
    var statement = mock(Statement.class);
    var holder = new ConnectionHolder(physical) {
      @Override
      public boolean hasTimeout() {
        return true;
      }

      @Override
      public int getTimeToLiveInSeconds() {
        return 9;
      }
    };
    given(physical.createStatement()).willReturn(statement);
    var proxy = new TransactionAwareDataSourceProxy(source);
    TransactionSynchronizationManager.bindResource(source, holder);
    try {
      try (var connection = proxy.getConnection()) {
        assertThat(connection.createStatement()).isSameAs(statement);
        verify(statement).setQueryTimeout(9);
      }
      verify(physical, never()).close();
      verifyNoInteractions(source);
      assertThat(holder.isOpen()).isFalse();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(source);
    }
  }

  private interface VendorConnection extends Connection {
  }

}
