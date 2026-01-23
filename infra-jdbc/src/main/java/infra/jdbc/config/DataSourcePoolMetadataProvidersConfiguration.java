/*
 * Copyright 2012-present the original author or authors.
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

package infra.jdbc.config;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import infra.jdbc.metadata.DataSourcePoolMetadataProvider;
import infra.jdbc.metadata.HikariDataSourcePoolMetadata;
import infra.jdbc.metadata.OracleUcpDataSourcePoolMetadata;
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
        var dbcpDataSource = DataSourceUnwrapper.unwrap(dataSource, BasicDataSourceMXBean.class, BasicDataSource.class);
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
