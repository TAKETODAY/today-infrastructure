/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.context.junit4;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.core.io.Resource;
import infra.dao.DataAccessException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.SqlParameterValue;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.lang.Assert;
import infra.test.annotation.Commit;
import infra.test.annotation.Rollback;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestExecutionListeners;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.jdbc.SqlScriptsTestExecutionListener;
import infra.test.context.junit4.rules.InfraClassRule;
import infra.test.context.junit4.rules.InfraMethodRule;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.test.context.transaction.TransactionalTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;
import infra.test.jdbc.JdbcTestUtils;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.Transactional;

/**
 * Abstract {@linkplain Transactional transactional} extension of
 * {@link AbstractJUnit4ContextTests} which adds convenience functionality
 * for JDBC access. Expects a {@link DataSource} bean and a
 * {@link PlatformTransactionManager} bean to be defined in the Infra
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
 * {@link AbstractJUnit4ContextTests}.
 *
 * <p>This class serves only as a convenience for extension.
 * <ul>
 * <li>If you do not wish for your test classes to be tied to a Infra-specific
 * class hierarchy, you may configure your own custom test classes by using
 * {@link InfraRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, etc.</li>
 * <li>If you wish to extend this class and use a runner other than the
 * {@link InfraRunner}, you can use
 * {@link InfraClassRule ApplicationClassRule} and
 * {@link InfraMethodRule ApplicationMethodRule}
 * and specify your runner of choice via {@link org.junit.runner.RunWith @RunWith(...)}.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see AbstractJUnit4ContextTests
 * @see ContextConfiguration
 * @see TestExecutionListeners
 * @see TransactionalTestExecutionListener
 * @see SqlScriptsTestExecutionListener
 * @see infra.transaction.annotation.Transactional
 * @see Commit
 * @see Rollback
 * @see BeforeTransaction
 * @see AfterTransaction
 * @see JdbcTestUtils
 * @since 4.0
 */
@TestExecutionListeners(listeners = { MockTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class }, inheritListeners = false)
@Transactional
public abstract class AbstractTransactionalJUnit4ContextTests extends AbstractJUnit4ContextTests {

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
   * {@link SqlParameterValue SqlParameterValue}
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
   * @param sqlResourcePath the Infra resource path for the SQL script
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
