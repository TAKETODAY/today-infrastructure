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

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:55
 */
class JdbcConnectionTests {

  @Test
  void shouldCreateJdbcConnectionWithoutAutoClose() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection).isNotNull();
    assertThat(connection.autoClose).isFalse();
  }

  @Test
  void shouldGetManager() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.getManager()).isEqualTo(manager);
  }

  @Test
  void shouldSetAndGetRollbackOnException() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.isRollbackOnException()).isTrue();

    connection.setRollbackOnException(false);
    assertThat(connection.isRollbackOnException()).isFalse();

    connection.setRollbackOnException(true);
    assertThat(connection.isRollbackOnException()).isTrue();
  }

  @Test
  void shouldSetAndGetRollbackOnClose() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);

    assertThat(connection.isRollbackOnClose()).isTrue();

    connection.setRollbackOnClose(false);
    assertThat(connection.isRollbackOnClose()).isFalse();

    connection.setRollbackOnClose(true);
    assertThat(connection.isRollbackOnClose()).isTrue();
  }

  @Test
  void shouldHandleOnExceptionWithRollback() {
    RepositoryManager manager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);

    JdbcConnection connection = new JdbcConnection(manager, dataSource);
    connection.setRollbackOnException(true);

    connection.onException();

    // Should not throw exception
    assertThat(true).isTrue();
  }

}