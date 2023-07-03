/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.jdbc.metadata;

import org.junit.jupiter.api.Test;

import cn.taketoday.jdbc.config.DataSourceBuilder;
import cn.taketoday.jdbc.core.ConnectionCallback;
import cn.taketoday.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link DataSourcePoolMetadata} tests.
 *
 * @param <D> the data source pool metadata type
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
abstract class AbstractDataSourcePoolMetadataTests<D extends AbstractDataSourcePoolMetadata<?>> {

  /**
   * Return a data source metadata instance with a min size of 0 and max size of 2. Idle
   * connections are not reclaimed immediately.
   *
   * @return the data source metadata
   */
  protected abstract D getDataSourceMetadata();

  @Test
  void getMaxPoolSize() {
    assertThat(getDataSourceMetadata().getMax()).isEqualTo(2);
  }

  @Test
  void getMinPoolSize() {
    assertThat(getDataSourceMetadata().getMin()).isEqualTo(0);
  }

  @Test
  void getPoolSizeNoConnection() {
    // Make sure the pool is initialized
    JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
    jdbcTemplate.execute((ConnectionCallback<Void>) (connection) -> null);
    assertThat(getDataSourceMetadata().getActive()).isEqualTo(0);
    assertThat(getDataSourceMetadata().getUsage()).isEqualTo(0f);
  }

  @Test
  void getPoolSizeOneConnection() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
    jdbcTemplate.execute((ConnectionCallback<Void>) (connection) -> {
      assertThat(getDataSourceMetadata().getActive()).isEqualTo(1);
      assertThat(getDataSourceMetadata().getUsage()).isEqualTo(0.5f);
      return null;
    });
  }

  @Test
  void getIdle() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
    jdbcTemplate.execute((ConnectionCallback<Void>) (connection) -> null);
    assertThat(getDataSourceMetadata().getIdle()).isEqualTo(1);
  }

  @Test
  void getPoolSizeTwoConnections() {
    final JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceMetadata().getDataSource());
    jdbcTemplate.execute((ConnectionCallback<Void>) (connection) -> {
      jdbcTemplate.execute((ConnectionCallback<Void>) (connection1) -> {
        assertThat(getDataSourceMetadata().getActive()).isEqualTo(2);
        assertThat(getDataSourceMetadata().getUsage()).isEqualTo(1.0f);
        return null;
      });
      return null;
    });
  }

  @Test
  abstract void getValidationQuery() throws Exception;

  @Test
  abstract void getDefaultAutoCommit() throws Exception;

  protected DataSourceBuilder<?> initializeBuilder() {
    return DataSourceBuilder.create().driverClassName("org.hsqldb.jdbc.JDBCDriver").url("jdbc:hsqldb:mem:test")
            .username("sa");
  }

}
