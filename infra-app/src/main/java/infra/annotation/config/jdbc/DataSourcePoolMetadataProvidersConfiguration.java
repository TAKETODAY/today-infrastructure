/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.annotation.config.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

import infra.app.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import infra.app.jdbc.metadata.DataSourcePoolMetadataProvider;
import infra.app.jdbc.metadata.HikariDataSourcePoolMetadata;
import infra.app.jdbc.metadata.OracleUcpDataSourcePoolMetadata;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.jdbc.config.DataSourceUnwrapper;
import infra.stereotype.Component;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;

/**
 * Register the {@link DataSourcePoolMetadataProvider} instances for the supported data
 * sources.
 *
 * @author Stephane Nicoll
 * @author Fabio Grassi
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
public class DataSourcePoolMetadataProvidersConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(HikariDataSource.class)
  static class HikariPoolDataSourceMetadataProviderConfiguration {

    @Component
    static DataSourcePoolMetadataProvider hikariPoolDataSourceMetadataProvider() {
      return (dataSource) -> {
        var hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
                HikariDataSource.class);
        if (hikariDataSource != null) {
          return new HikariDataSourcePoolMetadata(hikariDataSource);
        }
        return null;
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(BasicDataSource.class)
  static class CommonsDbcp2PoolDataSourceMetadataProviderConfiguration {

    @Component
    static DataSourcePoolMetadataProvider commonsDbcp2PoolDataSourceMetadataProvider() {
      return (dataSource) -> {
        var dbcpDataSource = DataSourceUnwrapper.unwrap(
                dataSource, BasicDataSourceMXBean.class, BasicDataSource.class);
        if (dbcpDataSource != null) {
          return new CommonsDbcp2DataSourcePoolMetadata(dbcpDataSource);
        }
        return null;
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ PoolDataSource.class, OracleConnection.class })
  static class OracleUcpPoolDataSourceMetadataProviderConfiguration {

    @Component
    static DataSourcePoolMetadataProvider oracleUcpPoolDataSourceMetadataProvider() {
      return (dataSource) -> {
        PoolDataSource ucpDataSource = DataSourceUnwrapper.unwrap(dataSource, PoolDataSource.class);
        if (ucpDataSource != null) {
          return new OracleUcpDataSourcePoolMetadata(ucpDataSource);
        }
        return null;
      };
    }

  }

}
