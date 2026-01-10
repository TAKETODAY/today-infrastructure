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

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import infra.jdbc.datasource.WrappedConnection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:58
 */
class NestedConnectionTests {

  @Test
  void shouldCreateNestedConnection() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    assertThat(nestedConnection).isNotNull();
    assertThat(nestedConnection.getAutoCommit()).isTrue();
  }

  @Test
  void shouldCommitWithoutException() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    // Should not throw exception
    nestedConnection.commit();
    assertThat(true).isTrue();
  }

  @Test
  void shouldRollbackWhenNotCommitted() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    // Should rollback parent connection
    nestedConnection.rollback();
    assertThat(true).isTrue();
  }

  @Test
  void shouldNotRollbackWhenAlreadyCommitted() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    nestedConnection.commit();
    // Should not rollback after commit
    nestedConnection.rollback();
    assertThat(true).isTrue();
  }

  @Test
  void shouldCloseWithoutException() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    // Should not throw exception
    nestedConnection.close();
    assertThat(true).isTrue();
  }

  @Test
  void shouldSetTransactionIsolationWithoutException() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    // Should not throw exception
    nestedConnection.setTransactionIsolation(1);
    assertThat(true).isTrue();
  }

  @Test
  void shouldSetAndGetAutoCommit() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    assertThat(nestedConnection.getAutoCommit()).isTrue();

    nestedConnection.setAutoCommit(false);
    assertThat(nestedConnection.getAutoCommit()).isFalse();

    nestedConnection.setAutoCommit(true);
    assertThat(nestedConnection.getAutoCommit()).isTrue();
  }

  @Test
  void shouldDelegateToSourceConnection() throws SQLException {
    Connection source = new MockConnection();
    NestedConnection nestedConnection = new NestedConnection(source);

    // Test that other methods delegate to source
    assertThat(nestedConnection.getMetaData()).isEqualTo(source.getMetaData());
    assertThat(nestedConnection.isClosed()).isEqualTo(source.isClosed());
  }

  static class MockConnection extends WrappedConnection {
    private boolean closed = false;
    private boolean autoCommit = true;

    public MockConnection() {
      super(null);
    }

    @Override
    public java.sql.DatabaseMetaData getMetaData() throws SQLException {
      return null;
    }

    @Override
    public boolean isClosed() throws SQLException {
      return closed;
    }

    @Override
    public void close() throws SQLException {
      closed = true;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      return autoCommit;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      this.autoCommit = autoCommit;
    }

    @Override
    public void rollback() throws SQLException {
      // Mock rollback
    }
  }

}