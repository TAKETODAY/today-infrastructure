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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.jdbc;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPoolMBean;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.framework.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import cn.taketoday.framework.jdbc.metadata.DataSourcePoolMetadataProvider;
import cn.taketoday.framework.jdbc.metadata.HikariDataSourcePoolMetadata;
import cn.taketoday.framework.jdbc.metadata.OracleUcpDataSourcePoolMetadata;
import cn.taketoday.framework.jdbc.metadata.TomcatDataSourcePoolMetadata;
import cn.taketoday.jdbc.config.DataSourceUnwrapper;
import cn.taketoday.stereotype.Component;
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
  @ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
  static class TomcatDataSourcePoolMetadataProviderConfiguration {

    @Component
    static DataSourcePoolMetadataProvider tomcatPoolDataSourceMetadataProvider() {
      return (dataSource) -> {
        var tomcatDataSource = DataSourceUnwrapper.unwrap(dataSource,
                ConnectionPoolMBean.class, org.apache.tomcat.jdbc.pool.DataSource.class);
        if (tomcatDataSource != null) {
          return new TomcatDataSourcePoolMetadata(tomcatDataSource);
        }
        return null;
      };
    }

  }

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
