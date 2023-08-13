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

import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnCheckpointRestore;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.framework.jdbc.HikariCheckpointRestoreLifecycle;
import cn.taketoday.jdbc.config.DatabaseDriver;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.StringUtils;
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

  @SuppressWarnings("unchecked")
  protected static <T> T createDataSource(DataSourceProperties properties, Class<? extends DataSource> type) {
    return (T) properties.initializeDataSourceBuilder().type(type).build();
  }

  /**
   * Tomcat Pool DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource", matchIfMissing = true)
  static class Tomcat {

    @Bean
    @ConfigurationProperties(prefix = "datasource.tomcat")
    org.apache.tomcat.jdbc.pool.DataSource dataSource(DataSourceProperties properties) {
      org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(properties, org.apache.tomcat.jdbc.pool.DataSource.class);
      DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(properties.determineUrl());
      String validationQuery = databaseDriver.getValidationQuery();
      if (validationQuery != null) {
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery(validationQuery);
      }
      return dataSource;
    }

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
    @ConfigurationProperties(prefix = "datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) {
      HikariDataSource dataSource = createDataSource(properties, HikariDataSource.class);
      if (StringUtils.hasText(properties.getName())) {
        dataSource.setPoolName(properties.getName());
      }
      return dataSource;
    }

    @Component
    @ConditionalOnCheckpointRestore
    HikariCheckpointRestoreLifecycle hikariCheckpointRestoreLifecycle(HikariDataSource hikariDataSource) {
      return new HikariCheckpointRestoreLifecycle(hikariDataSource);
    }

  }

  /**
   * DBCP DataSource configuration.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
  @ConditionalOnMissingBean(DataSource.class)
  @ConditionalOnProperty(name = "datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource", matchIfMissing = true)
  static class Dbcp2 {

    @Bean
    @ConfigurationProperties(prefix = "datasource.dbcp2")
    org.apache.commons.dbcp2.BasicDataSource dataSource(DataSourceProperties properties) {
      return createDataSource(properties, org.apache.commons.dbcp2.BasicDataSource.class);
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

    @Bean
    @ConfigurationProperties(prefix = "datasource.oracleucp")
    PoolDataSourceImpl dataSource(DataSourceProperties properties) throws SQLException {
      PoolDataSourceImpl dataSource = createDataSource(properties, PoolDataSourceImpl.class);
      dataSource.setValidateConnectionOnBorrow(true);
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

    @Bean
    DataSource dataSource(DataSourceProperties properties) {
      return properties.initializeDataSourceBuilder().build();
    }

  }

}
