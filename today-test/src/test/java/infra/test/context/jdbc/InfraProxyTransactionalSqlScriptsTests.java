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

package infra.test.context.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.InfraProxy;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional integration tests for {@link Sql @Sql} support when the
 * {@link DataSource} is wrapped in a proxy that implements
 * {@link InfraProxy}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@DirtiesContext
class InfraProxyTransactionalSqlScriptsTests extends AbstractTransactionalTests {

  @BeforeEach
  void preconditions(@Autowired DataSource dataSource, @Autowired DataSourceTransactionManager transactionManager) {
    assertThat(dataSource).isNotEqualTo(transactionManager.getDataSource());
    assertThat(transactionManager.getDataSource()).isNotEqualTo(dataSource);
    assertThat(transactionManager.getDataSource()).isInstanceOf(InfraProxy.class);
  }

  @Test
  @Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
  void methodLevelScripts() {
    assertNumUsers(2);
  }

  @Configuration
  static class DatabaseConfig {

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(wrapDataSource(dataSource));
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()//
              .setName("empty-sql-scripts-test-db")//
              .build();
    }

  }

  private static DataSource wrapDataSource(DataSource dataSource) {
    return (DataSource) Proxy.newProxyInstance(
            InfraProxyTransactionalSqlScriptsTests.class.getClassLoader(),
            new Class<?>[] { DataSource.class, InfraProxy.class },
            new DataSourceInvocationHandler(dataSource));
  }

  private static class DataSourceInvocationHandler implements InvocationHandler {

    private final DataSource dataSource;

    DataSourceInvocationHandler(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "equals":
          return (proxy == args[0]);
        case "hashCode":
          return System.identityHashCode(proxy);
        case "getWrappedObject":
          return this.dataSource;
        default:
          try {
            return method.invoke(this.dataSource, args);
          }
          catch (InvocationTargetException ex) {
            throw ex.getTargetException();
          }
      }
    }
  }

}
