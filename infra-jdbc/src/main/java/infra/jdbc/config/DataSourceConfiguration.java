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

import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

import javax.sql.DataSource;

import infra.beans.factory.ObjectProvider;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnCheckpointRestore;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.ConfigurationProperties;
import infra.core.env.Environment;
import infra.stereotype.Component;
import infra.util.StringUtils;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSourceImpl;

/**
 * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Fabio Grassi
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 18:03
 */
abstract class DataSourceConfiguration {

  private static <T extends DataSource> T createDataSource(JdbcConnectionDetails connectionDetails,
          @Nullable Class<T> type, ClassLoader classLoader) {
    return createDataSource(connectionDetails, type, classLoader, true);
  }

  private static <T extends DataSource> T createDataSource(JdbcConnectionDetails connectionDetails,
          @Nullable Class<T> type, ClassLoader classLoader, boolean applyDriverClassName) {
    DataSourceBuilder<T> builder = DataSourceBuilder.create(classLoader).type(type);
    if (applyDriverClassName) {
      builder.driverClassName(connectionDetails.getDriverClassName());
    }
    return builder.url(connectionDetails.getJdbcUrl())
            .username(connectionDetails.getUsername())
            .password(connectionDetails.getPassword())
            .build();
  }

  /**
   * Hikari DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(HikariDataSource.class)
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
  static class Hikari {

    @Component
    static HikariJdbcConnectionDetailsBeanPostProcessor jdbcConnectionDetailsHikariBeanPostProcessor(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
      return new HikariJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
    }

    @Component
    @ConfigurationProperties("datasource.hikari")
    static HikariDataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails, Environment environment) {
      String dataSourceClassName = environment.getProperty("datasource.hikari.data-source-class-name");
      HikariDataSource dataSource = createDataSource(connectionDetails, HikariDataSource.class,
              properties.getClassLoader(), dataSourceClassName == null);
      if (StringUtils.hasText(properties.getName())) {
        dataSource.setPoolName(properties.getName());
      }
      return dataSource;
    }

    @Component
    @ConditionalOnCheckpointRestore
    static HikariCheckpointRestoreLifecycle hikariCheckpointRestoreLifecycle(DataSource hikariDataSource,
            ConfigurableApplicationContext applicationContext) {
      return new HikariCheckpointRestoreLifecycle(hikariDataSource, applicationContext);
    }

  }

  /**
   * DBCP DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(BasicDataSource.class)
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource", matchIfMissing = true)
  static class Dbcp2 {

    @Component
    static Dbcp2JdbcConnectionDetailsBeanPostProcessor dbcp2JdbcConnectionDetailsBeanPostProcessor(
            ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
      return new Dbcp2JdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
    }

    @Component
    @ConfigurationProperties("datasource.dbcp2")
    static BasicDataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
      return createDataSource(connectionDetails, BasicDataSource.class, properties.getClassLoader());
    }

  }

  /**
   * Oracle UCP DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type", havingValue = "oracle.ucp.jdbc.PoolDataSource", matchIfMissing = true)
  static class OracleUcp {

    @Component
    static OracleUcpJdbcConnectionDetailsBeanPostProcessor oracleUcpJdbcConnectionDetailsBeanPostProcessor(
            ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
      return new OracleUcpJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
    }

    @Component
    @ConfigurationProperties("datasource.oracleucp")
    static PoolDataSourceImpl dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) throws SQLException {
      PoolDataSourceImpl dataSource = createDataSource(connectionDetails, PoolDataSourceImpl.class,
              properties.getClassLoader());

      if (StringUtils.hasText(properties.getName())) {
        dataSource.setConnectionPoolName(properties.getName());
      }
      return dataSource;
    }

  }

  /**
   * Generic DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type")
  static class Generic {

    @Component
    static DataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
      return createDataSource(connectionDetails, properties.getType(), properties.getClassLoader());
    }

  }

}
