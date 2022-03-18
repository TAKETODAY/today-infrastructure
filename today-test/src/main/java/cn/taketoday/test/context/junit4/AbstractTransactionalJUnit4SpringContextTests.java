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

package cn.taketoday.test.context.junit4;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.Resource;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.junit4.rules.SpringClassRule;
import cn.taketoday.test.context.junit4.rules.SpringMethodRule;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.annotation.Commit;
import cn.taketoday.test.annotation.Rollback;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.event.ApplicationEventsTestExecutionListener;
import cn.taketoday.test.context.event.EventPublishingTestExecutionListener;
import cn.taketoday.test.context.jdbc.SqlScriptsTestExecutionListener;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;
import cn.taketoday.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.TransactionalTestExecutionListener;
import cn.taketoday.test.context.web.ServletTestExecutionListener;
import cn.taketoday.test.jdbc.JdbcTestUtils;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.Transactional;

/**
 * Abstract {@linkplain Transactional transactional} extension of
 * {@link AbstractJUnit4SpringContextTests} which adds convenience functionality
 * for JDBC access. Expects a {@link DataSource} bean and a
 * {@link PlatformTransactionManager} bean to be defined in the Spring
 * {@linkplain ApplicationContext application context}.
 *
 * <p>This class exposes a {@link JdbcTemplate} and provides an easy way to
 * {@linkplain #countRowsInTable count the number of rows in a table}
 * (potentially {@linkplain #countRowsInTableWhere with a WHERE clause}),
 * {@linkplain #deleteFromTables delete from tables},
 * {@linkplain #dropTables drop tables}, and
 * {@linkplain #executeSqlScript execute SQL scripts} within a transaction.
 *
 * <p>Concrete subclasses must fulfill the same requirements outlined in
 * {@link AbstractJUnit4SpringContextTests}.
 *
 * <p>The following {@link TestExecutionListener
 * TestExecutionListeners} are configured by default:
 *
 * <ul>
 * <li>{@link ServletTestExecutionListener}
 * <li>{@link DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link ApplicationEventsTestExecutionListener}</li>
 * <li>{@link DependencyInjectionTestExecutionListener}
 * <li>{@link DirtiesContextTestExecutionListener}
 * <li>{@link TransactionalTestExecutionListener}
 * <li>{@link SqlScriptsTestExecutionListener}
 * <li>{@link EventPublishingTestExecutionListener}
 * </ul>
 *
 * <p>This class serves only as a convenience for extension.
 * <ul>
 * <li>If you do not wish for your test classes to be tied to a Spring-specific
 * class hierarchy, you may configure your own custom test classes by using
 * {@link SpringRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, etc.</li>
 * <li>If you wish to extend this class and use a runner other than the
 * {@link SpringRunner}, as of Spring Framework 4.2 you can use
 * {@link SpringClassRule SpringClassRule} and
 * {@link SpringMethodRule SpringMethodRule}
 * and specify your runner of choice via {@link org.junit.runner.RunWith @RunWith(...)}.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see AbstractJUnit4SpringContextTests
 * @see ContextConfiguration
 * @see TestExecutionListeners
 * @see TransactionalTestExecutionListener
 * @see SqlScriptsTestExecutionListener
 * @see cn.taketoday.transaction.annotation.Transactional
 * @see Commit
 * @see Rollback
 * @see BeforeTransaction
 * @see AfterTransaction
 * @see JdbcTestUtils
 * @see AbstractTransactionalTestNGSpringContextTests
 * @since 4.0
 */
@TestExecutionListeners(listeners = { ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class }, inheritListeners = false)
@Transactional
public abstract class AbstractTransactionalJUnit4SpringContextTests extends AbstractJUnit4SpringContextTests {

  /**
   * The {@code JdbcTemplate} that this base class manages, available to subclasses.
   *
   * @since 4.0
   */
  protected final JdbcTemplate jdbcTemplate = new JdbcTemplate();

  @Nullable
  private String sqlScriptEncoding;

  /**
   * Set the {@code DataSource}, typically provided via Dependency Injection.
   * <p>This method also instantiates the {@link #jdbcTemplate} instance variable.
   */
  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate.setDataSource(dataSource);
  }

  /**
   * Specify the encoding for SQL scripts, if different from the platform encoding.
   *
   * @see #executeSqlScript
   */
  public void setSqlScriptEncoding(String sqlScriptEncoding) {
    this.sqlScriptEncoding = sqlScriptEncoding;
  }

  /**
   * Convenience method for counting the rows in the given table.
   *
   * @param tableName table name to count rows in
   * @return the number of rows in the table
   * @see JdbcTestUtils#countRowsInTable
   */
  protected int countRowsInTable(String tableName) {
    return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
  }

  /**
   * Convenience method for counting the rows in the given table, using the
   * provided {@code WHERE} clause.
   * <p>See the Javadoc for {@link JdbcTestUtils#countRowsInTableWhere} for details.
   *
   * @param tableName the name of the table to count rows in
   * @param whereClause the {@code WHERE} clause to append to the query
   * @return the number of rows in the table that match the provided
   * {@code WHERE} clause
   * @see JdbcTestUtils#countRowsInTableWhere
   * @since 4.0
   */
  protected int countRowsInTableWhere(String tableName, String whereClause) {
    return JdbcTestUtils.countRowsInTableWhere(this.jdbcTemplate, tableName, whereClause);
  }

  /**
   * Convenience method for deleting all rows from the specified tables.
   * <p>Use with caution outside of a transaction!
   *
   * @param names the names of the tables from which to delete
   * @return the total number of rows deleted from all specified tables
   * @see JdbcTestUtils#deleteFromTables
   */
  protected int deleteFromTables(String... names) {
    return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
  }

  /**
   * Convenience method for deleting all rows from the given table, using the
   * provided {@code WHERE} clause.
   * <p>Use with caution outside of a transaction!
   * <p>See the Javadoc for {@link JdbcTestUtils#deleteFromTableWhere} for details.
   *
   * @param tableName the name of the table to delete rows from
   * @param whereClause the {@code WHERE} clause to append to the query
   * @param args arguments to bind to the query (leaving it to the {@code
   * PreparedStatement} to guess the corresponding SQL type); may also contain
   * {@link cn.taketoday.jdbc.core.SqlParameterValue SqlParameterValue}
   * objects which indicate not only the argument value but also the SQL type
   * and optionally the scale.
   * @return the number of rows deleted from the table
   * @see JdbcTestUtils#deleteFromTableWhere
   * @since 4.0
   */
  protected int deleteFromTableWhere(String tableName, String whereClause, Object... args) {
    return JdbcTestUtils.deleteFromTableWhere(this.jdbcTemplate, tableName, whereClause, args);
  }

  /**
   * Convenience method for dropping all of the specified tables.
   * <p>Use with caution outside of a transaction!
   *
   * @param names the names of the tables to drop
   * @see JdbcTestUtils#dropTables
   * @since 4.0
   */
  protected void dropTables(String... names) {
    JdbcTestUtils.dropTables(this.jdbcTemplate, names);
  }

  /**
   * Execute the given SQL script.
   * <p>Use with caution outside of a transaction!
   * <p>The script will normally be loaded by classpath.
   * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
   *
   * @param sqlResourcePath the Spring resource path for the SQL script
   * @param continueOnError whether or not to continue without throwing an
   * exception in the event of an error
   * @throws DataAccessException if there is an error executing a statement
   * @see ResourceDatabasePopulator
   * @see #setSqlScriptEncoding
   */
  protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
    DataSource ds = this.jdbcTemplate.getDataSource();
    Assert.state(ds != null, "No DataSource set");
    Assert.state(this.applicationContext != null, "No ApplicationContext set");
    Resource resource = this.applicationContext.getResource(sqlResourcePath);
    new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(ds);
  }

}
