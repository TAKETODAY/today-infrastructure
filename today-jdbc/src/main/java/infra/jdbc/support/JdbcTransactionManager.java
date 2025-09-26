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

package infra.jdbc.support;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.sql.SQLException;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;

/**
 * {@link JdbcAccessor}-aligned subclass of the plain {@link DataSourceTransactionManager},
 * adding common JDBC exception translation for the commit and rollback step.
 * Typically used in combination with {@link JdbcTemplate}
 * which applies the same {@link SQLExceptionTranslator} infrastructure by default.
 *
 * <p>Exception translation is specifically relevant for commit steps in serializable
 * transactions (e.g. on Postgres) where concurrency failures may occur late on commit.
 * This allows for throwing {@link infra.dao.ConcurrencyFailureException} to
 * callers instead of {@link infra.transaction.TransactionSystemException}.
 *
 * <p>Analogous to {@code HibernateTransactionManager} and {@code JpaTransactionManager},
 * this transaction manager may throw {@link DataAccessException} from {@link #commit}
 * and possibly also from {@link #rollback}. Calling code should be prepared for handling
 * such exceptions next to {@link infra.transaction.TransactionException},
 * which is generally sensible since {@code TransactionSynchronization} implementations
 * may also throw such exceptions in their {@code flush} and {@code beforeCommit} phases.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataSourceTransactionManager
 * @see #setDataSource
 * @see #setExceptionTranslator
 * @since 4.0
 */
public class JdbcTransactionManager extends DataSourceTransactionManager {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private volatile SQLExceptionTranslator exceptionTranslator;

  private boolean lazyInit = true;

  /**
   * Create a new JdbcTransactionManager instance.
   * A DataSource has to be set to be able to use it.
   *
   * @see #setDataSource
   */
  public JdbcTransactionManager() {
    super();
  }

  /**
   * Create a new JdbcTransactionManager instance.
   *
   * @param dataSource the JDBC DataSource to manage transactions for
   */
  public JdbcTransactionManager(DataSource dataSource) {
    this();
    setDataSource(dataSource);
    afterPropertiesSet();
  }

  /**
   * Specify the database product name for the DataSource that this transaction manager
   * uses. This allows to initialize an SQLErrorCodeSQLExceptionTranslator without
   * obtaining a Connection from the DataSource to get the meta-data.
   *
   * @param dbName the database product name that identifies the error codes entry
   * @see JdbcAccessor#setDatabaseProductName
   * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public void setDatabaseProductName(String dbName) {
    if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
      this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
    }
    else {
      this.exceptionTranslator = new SQLExceptionSubclassTranslator();
    }
  }

  /**
   * Set the exception translator for this instance.
   * <p>If no custom translator is provided, a default
   * {@link SQLErrorCodeSQLExceptionTranslator} is used
   * which examines the SQLException's vendor-specific error code.
   *
   * @see JdbcAccessor#setExceptionTranslator
   * @see SQLErrorCodeSQLExceptionTranslator
   */
  public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * Return the exception translator for this instance.
   * <p>Creates a default {@link SQLErrorCodeSQLExceptionTranslator}
   * for the specified DataSource if none set.
   *
   * @see #getDataSource()
   */
  public SQLExceptionTranslator getExceptionTranslator() {
    SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
    if (exceptionTranslator != null) {
      return exceptionTranslator;
    }
    synchronized(this) {
      exceptionTranslator = this.exceptionTranslator;
      if (exceptionTranslator == null) {
        if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
          exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
        }
        else {
          exceptionTranslator = new SQLExceptionSubclassTranslator();
        }
        this.exceptionTranslator = exceptionTranslator;
      }
      return exceptionTranslator;
    }
  }

  /**
   * Set whether to lazily initialize the SQLExceptionTranslator for this transaction manager,
   * on first encounter of an SQLException. Default is "true"; can be switched to
   * "false" for initialization on startup.
   * <p>Early initialization just applies if {@code afterPropertiesSet()} is called.
   *
   * @see #getExceptionTranslator()
   * @see #afterPropertiesSet()
   */
  public void setLazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return whether to lazily initialize the SQLExceptionTranslator for this transaction manager.
   *
   * @see #getExceptionTranslator()
   */
  public boolean isLazyInit() {
    return this.lazyInit;
  }

  /**
   * Eagerly initialize the exception translator, if demanded,
   * creating a default one for the specified DataSource if none set.
   */
  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    if (!isLazyInit()) {
      getExceptionTranslator();
    }
  }

  /**
   * This implementation attempts to use the {@link SQLExceptionTranslator},
   * falling back to a {@link infra.transaction.TransactionSystemException}.
   *
   * @see #getExceptionTranslator()
   * @see DataSourceTransactionManager#translateException
   */
  @Override
  protected RuntimeException translateException(String task, SQLException ex) {
    DataAccessException dae = getExceptionTranslator().translate(task, null, ex);
    if (dae != null) {
      return dae;
    }
    return super.translateException(task, ex);
  }

}
