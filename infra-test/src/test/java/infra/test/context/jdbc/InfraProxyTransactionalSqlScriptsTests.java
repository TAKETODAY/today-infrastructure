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
