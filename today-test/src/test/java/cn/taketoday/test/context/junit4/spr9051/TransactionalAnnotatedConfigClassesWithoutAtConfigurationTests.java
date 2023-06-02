/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.junit4.spr9051;

import org.junit.Before;

import javax.sql.DataSource;

import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concrete implementation of {@link AbstractTransactionalAnnotatedConfigClassTests}
 * that does <b>not</b> use a true {@link Configuration @Configuration class} but
 * rather a <em>lite mode</em> configuration class (see the Javadoc for {@link Bean @Bean}
 * for details).
 *
 * @author Sam Brannen
 * @see Bean
 * @see TransactionalAnnotatedConfigClassWithAtConfigurationTests
 * @since 4.0
 */
@ContextConfiguration(classes = TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests.AnnotatedFactoryBeans.class)
public class TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests extends
        AbstractTransactionalAnnotatedConfigClassTests {

  /**
   * This is intentionally <b>not</b> annotated with {@code @Configuration}.
   *
   * <p>Consequently, this class contains <i>annotated factory bean methods</i>
   * instead of standard singleton bean methods.
   */
  // @Configuration
  static class AnnotatedFactoryBeans {

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

    /**
     * Since this method does not reside in a true {@code @Configuration class},
     * it acts as a factory method when invoked directly (e.g., from
     * {@link #transactionManager()}) and as a singleton bean when retrieved
     * through the application context (e.g., when injected into the test
     * instance). The result is that this method will be called twice:
     *
     * <ol>
     * <li>once <em>indirectly</em> by the {@link TransactionalTestExecutionListener}
     * when it retrieves the {@link PlatformTransactionManager} from the
     * application context</li>
     * <li>and again when the {@link DataSource} is injected into the test
     * instance in {@link AbstractTransactionalAnnotatedConfigClassTests#setDataSource(DataSource)}.</li>
     * </ol>
     *
     * Consequently, the {@link JdbcTemplate} used by this test instance and
     * the {@link PlatformTransactionManager} used by the Infra TestContext
     * Framework will operate on two different {@code DataSource} instances,
     * which is almost certainly not the desired or intended behavior.
     */
    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()//
              .addScript("classpath:/cn/taketoday/test/jdbc/schema.sql")//
              // Ensure that this in-memory database is only used by this class:
              .setName(getClass().getName())//
              .build();
    }

  }

  @Before
  public void compareDataSources() throws Exception {
    // NOTE: the two DataSource instances are NOT the same!
    assertThat(dataSourceViaInjection).isNotSameAs(dataSourceFromTxManager);
  }

  /**
   * Overrides {@code afterTransaction()} in order to assert a different result.
   *
   * <p>See in-line comments for details.
   *
   * @see AbstractTransactionalAnnotatedConfigClassTests#afterTransaction()
   * @see AbstractTransactionalAnnotatedConfigClassTests#modifyTestDataWithinTransaction()
   */
  @AfterTransaction
  @Override
  public void afterTransaction() {
    assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);

    // NOTE: We would actually expect that there are now ZERO entries in the
    // person table, since the transaction is rolled back by the framework;
    // however, since our JdbcTemplate and the transaction manager used by
    // the TestContext Framework use two different DataSource
    // instances, our insert statements were executed in transactions that
    // are not controlled by the test framework. Consequently, there was no
    // rollback for the two insert statements in
    // modifyTestDataWithinTransaction().
    //
    assertNumRowsInPersonTable(2, "after a transactional test method");
  }

}
