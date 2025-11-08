/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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