/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/11 23:19
 */
class ShardingKeyDataSourceAdapterTests {

  private final Connection connection = mock();

  private final Connection shardConnection = mock();

  private final DataSource dataSource = mock();

  private final ConnectionBuilder connectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);

  private final ConnectionBuilder shardConnectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);

  private final ShardingKey shardingKey = mock();

  private final ShardingKey superShardingKey = mock();

  private final ShardingKeyProvider shardingKeyProvider = new ShardingKeyProvider() {
    @Override
    public ShardingKey getShardingKey() throws SQLException {
      return shardingKey;
    }

    @Override
    public ShardingKey getSuperShardingKey() throws SQLException {
      return superShardingKey;
    }
  };

  @BeforeEach
  void setup() throws SQLException {
    given(dataSource.createConnectionBuilder()).willReturn(connectionBuilder);
    when(connectionBuilder.shardingKey(null).superShardingKey(null)).thenReturn(connectionBuilder);
    when(connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey))
            .thenReturn(shardConnectionBuilder);
  }

  @Test
  void getConnectionNoKeyProvider() throws SQLException {
    ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

    when(connectionBuilder.build()).thenReturn(connection);

    assertThat(dataSourceAdapter.getConnection()).isEqualTo(connection);
  }

  @Test
  void getConnectionWithKeyProvider() throws SQLException {
    ShardingKeyDataSourceAdapter dataSourceAdapter =
            new ShardingKeyDataSourceAdapter(dataSource, shardingKeyProvider);

    when(shardConnectionBuilder.build()).thenReturn(shardConnection);

    assertThat(dataSourceAdapter.getConnection()).isEqualTo(shardConnection);
  }

  @Test
  void getConnectionWithCredentialsNoKeyProvider() throws SQLException {
    ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

    String username = "Anir";
    String password = "spring";

    when(connectionBuilder.user(username).password(password).build()).thenReturn(connection);

    assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(connection);
  }

  @Test
  void getConnectionWithCredentialsAndKeyProvider() throws SQLException {
    ShardingKeyDataSourceAdapter dataSourceAdapter =
            new ShardingKeyDataSourceAdapter(dataSource, shardingKeyProvider);

    String username = "mbekraou";
    String password = "jdbc";

    when(shardConnectionBuilder.user(username).password(password).build()).thenReturn(connection);

    assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(connection);
  }

}