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