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

package infra.test.context.junit4.spr9051;

import org.junit.Before;

import javax.sql.DataSource;

import infra.beans.testfixture.beans.Employee;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.context.ContextConfiguration;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concrete implementation of {@link AbstractTransactionalAnnotatedConfigClassTests}
 * that uses a true {@link Configuration @Configuration class}.
 *
 * @author Sam Brannen
 * @see TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests
 * @since 4.0
 */
@ContextConfiguration
public class TransactionalAnnotatedConfigClassWithAtConfigurationTests extends
        AbstractTransactionalAnnotatedConfigClassTests {

  /**
   * This is <b>intentionally</b> annotated with {@code @Configuration}.
   *
   * <p>Consequently, this class contains standard singleton bean methods
   * instead of <i>annotated factory bean methods</i>.
   */
  @Configuration
  static class Config {

    @Bean
    public Employee employee() {
      Employee employee = new Employee();
      employee.setName("John Smith");
      employee.setAge(42);
      employee.setCompany("Acme Widgets, Inc.");
      return employee;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
      return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()//
              .addScript("classpath:/infra/test/jdbc/schema.sql")//
              // Ensure that this in-memory database is only used by this class:
              .setName(getClass().getName())//
              .build();
    }

  }

  @Before
  public void compareDataSources() throws Exception {
    // NOTE: the two DataSource instances ARE the same!
    assertThat(dataSourceViaInjection).isSameAs(dataSourceFromTxManager);
  }

}
