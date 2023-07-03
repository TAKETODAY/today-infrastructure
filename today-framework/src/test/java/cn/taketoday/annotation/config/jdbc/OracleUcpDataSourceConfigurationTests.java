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

package cn.taketoday.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} with Oracle UCP.
 *
 * @author Fabio Grassi
 * @author Stephane Nicoll
 */
class OracleUcpDataSourceConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
          .withPropertyValues("datasource.type=" + PoolDataSource.class.getName());

  @Test
  void testDataSourceExists() {
    this.contextRunner.run((context) -> {
      assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
      assertThat(context.getBeansOfType(PoolDataSourceImpl.class)).hasSize(1);
      try (Connection connection = context.getBean(DataSource.class).getConnection()) {
        assertThat(connection.isValid(1000)).isTrue();
      }
    });
  }

  @Test
  void testDataSourcePropertiesOverridden() {
    this.contextRunner.withPropertyValues("datasource.oracleucp.url=jdbc:foo//bar/spam",
            "datasource.oracleucp.max-idle-time=1234").run((context) -> {
      PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
      assertThat(ds.getURL()).isEqualTo("jdbc:foo//bar/spam");
      assertThat(ds.getMaxIdleTime()).isEqualTo(1234);
    });
  }

  @Test
  void testDataSourceConnectionPropertiesOverridden() {
    this.contextRunner.withPropertyValues("datasource.oracleucp.connection-properties.autoCommit=false")
            .run((context) -> {
              PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
              assertThat(ds.getConnectionProperty("autoCommit")).isEqualTo("false");
            });
  }

  @Test
  void testDataSourceDefaultsPreserved() {
    this.contextRunner.run((context) -> {
      PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
      assertThat(ds.getInitialPoolSize()).isEqualTo(0);
      assertThat(ds.getMinPoolSize()).isEqualTo(0);
      assertThat(ds.getMaxPoolSize()).isEqualTo(Integer.MAX_VALUE);
      assertThat(ds.getInactiveConnectionTimeout()).isEqualTo(0);
      assertThat(ds.getConnectionWaitTimeout()).isEqualTo(3);
      assertThat(ds.getTimeToLiveConnectionTimeout()).isEqualTo(0);
      assertThat(ds.getAbandonedConnectionTimeout()).isEqualTo(0);
      assertThat(ds.getTimeoutCheckInterval()).isEqualTo(30);
      assertThat(ds.getFastConnectionFailoverEnabled()).isFalse();
    });
  }

  @Test
  void nameIsAliasedToPoolName() {
    this.contextRunner.withPropertyValues("datasource.name=myDS").run((context) -> {
      PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
      assertThat(ds.getConnectionPoolName()).isEqualTo("myDS");
    });
  }

  @Test
  void poolNameTakesPrecedenceOverName() {
    this.contextRunner.withPropertyValues("datasource.name=myDS",
            "datasource.oracleucp.connection-pool-name=myOracleUcpDS").run((context) -> {
      PoolDataSourceImpl ds = context.getBean(PoolDataSourceImpl.class);
      assertThat(ds.getConnectionPoolName()).isEqualTo("myOracleUcpDS");
    });
  }

}
