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

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/2 21:13
 */
class SingleConnectionDataSourceTests {

  private final Connection connection = mock();

  @Test
  void plainConnection() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false);

    ds.getConnection().close();
    verify(connection, times(1)).close();
  }

  @Test
  void withAutoCloseable() throws Exception {
    try (SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false)) {
      ds.getConnection();
    }

    verify(connection, times(1)).close();
  }

  @Test
  void withSuppressClose() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);

    ds.getConnection().close();
    verify(connection, never()).close();

    ds.destroy();
    verify(connection, times(1)).close();

    given(connection.isClosed()).willReturn(true);
    assertThatExceptionOfType(SQLException.class).isThrownBy(ds::getConnection);
  }

  @Test
  void withRollbackBeforeClose() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);
    ds.setRollbackBeforeClose(true);

    ds.destroy();
    verify(connection, times(1)).rollback();
    verify(connection, times(1)).close();
  }

  @Test
  void withEnforcedAutoCommit() throws Exception {
    SingleConnectionDataSource ds = new SingleConnectionDataSource() {
      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties props) {
        return connection;
      }
    };
    ds.setUrl("url");
    ds.setAutoCommit(true);

    ds.getConnection();
    verify(connection, times(1)).setAutoCommit(true);

    ds.destroy();
    verify(connection, times(1)).close();
  }

  @Test
  void lazilyRecreatesSharedConnectionAfterReset() throws SQLException {
    Connection replacement = mock();
    try (var source = new SingleConnectionDataSource("jdbc:mock", "user", "password", true) {

      private boolean initialized;

      @Override
      protected Connection getConnectionFromDriverManager(String url, Properties properties) {
        assertThat(url).isEqualTo("jdbc:mock");
        assertThat(properties.getProperty("user")).isEqualTo("user");
        assertThat(properties.getProperty("password")).isEqualTo("password");
        if (initialized) {
          return replacement;
        }
        initialized = true;
        return connection;
      }
    }) {
      source.resetConnection();
      var first = source.getConnection("user", "password");
      assertThat(first.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(connection);
      assertThat(source.getConnection()).isSameAs(first);
      assertThat(source.shouldClose(first)).isFalse();
      assertThat(source.shouldClose(connection)).isFalse();
      assertThat(source.shouldClose(replacement)).isTrue();
      assertThatExceptionOfType(SQLException.class).isThrownBy(() -> source.getConnection("user", "wrong"))
              .withMessageContaining("custom username and password");
      assertThatExceptionOfType(SQLException.class).isThrownBy(() -> source.getConnection("other", "password"));

      source.resetConnection();
      verify(connection).close();
      var second = source.getConnection();
      assertThat(second).isNotSameAs(first);
      assertThat(second.unwrap(ConnectionProxy.class).getTargetConnection()).isSameAs(replacement);
      assertThat(source.shouldClose(first)).isTrue();
    }
    verify(replacement).close();
  }

  @Test
  void requiresUrlBeforeOpeningUnconfiguredConnection() {
    try (var source = new SingleConnectionDataSource()) {
      assertThatIllegalStateException().isThrownBy(source::getConnection).withMessageContaining("'url' property");
    }
  }

  @Test
  void stillAttemptsCloseAfterRollbackFailure() throws SQLException {
    var source = new SingleConnectionDataSource(connection, false);
    source.setRollbackBeforeClose(true);
    willThrow(new SQLException("rollback failed")).given(connection).rollback();
    willThrow(new SQLException("close failed")).given(connection).close();

    source.resetConnection();

    var order = inOrder(connection);
    order.verify(connection).rollback();
    order.verify(connection).close();
  }

  @Test
  void skipsRollbackForAutoCommitConnectionAndDelegatesVendorUnwrapping() throws SQLException {
    var source = new SingleConnectionDataSource(connection, true);
    source.setRollbackBeforeClose(true);
    given(connection.getAutoCommit()).willReturn(true);
    var vendor = mock(VendorConnection.class);
    given(connection.isWrapperFor(VendorConnection.class)).willReturn(true);
    given(connection.unwrap(VendorConnection.class)).willReturn(vendor);

    var proxy = source.getConnection();
    assertThat(proxy.isWrapperFor(ConnectionProxy.class)).isTrue();
    assertThat(proxy.isWrapperFor(VendorConnection.class)).isTrue();
    assertThat(proxy.unwrap(VendorConnection.class)).isSameAs(vendor);
    source.close();

    verify(connection, never()).rollback();
    verify(connection).close();
  }

  private interface VendorConnection extends Connection {
  }

}
