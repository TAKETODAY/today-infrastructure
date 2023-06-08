/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource;

import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionStatus;
import cn.taketoday.transaction.support.ResourceTransactionManager;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionSynchronizationUtils;

/**
 * {@link cn.taketoday.transaction.PlatformTransactionManager} implementation
 * for a single JDBC {@link javax.sql.DataSource}. This class is capable of working
 * in any environment with any JDBC driver, as long as the setup uses a
 * {@code javax.sql.DataSource} as its {@code Connection} factory mechanism.
 * Binds a JDBC {@code Connection} from the specified {@code DataSource} to the
 * current thread, potentially allowing for one thread-bound {@code Connection}
 * per {@code DataSource}.
 *
 * <p><b>Note: The {@code DataSource} that this transaction manager operates on
 * needs to return independent {@code Connection}s.</b> The {@code Connection}s
 * typically come from a connection pool but the {@code DataSource} must not return
 * specifically scoped or constrained {@code Connection}s. This transaction manager
 * will associate {@code Connection}s with thread-bound transactions, according
 * to the specified propagation behavior. It assumes that a separate, independent
 * {@code Connection} can be obtained even during an ongoing transaction.
 *
 * <p>Application code is required to retrieve the JDBC {@code Connection} via
 * {@link DataSourceUtils#getConnection(DataSource)} instead of a standard
 * EE-style {@link DataSource#getConnection()} call. Infra classes such as
 * {@link cn.taketoday.jdbc.core.JdbcTemplate} use this strategy implicitly.
 * If not used in combination with this transaction manager, the
 * {@link DataSourceUtils} lookup strategy behaves exactly like the native
 * {@code DataSource} lookup; it can thus be used in a portable fashion.
 *
 * <p>Alternatively, you can allow application code to work with the standard
 * EE-style lookup pattern {@link DataSource#getConnection()}, for example
 * for legacy code that is not aware of Infra at all. In that case, define a
 * {@link TransactionAwareDataSourceProxy} for your target {@code DataSource},
 * and pass that proxy {@code DataSource} to your DAOs which will automatically
 * participate in Infra-managed transactions when accessing it.
 *
 * <p>Supports custom isolation levels, and timeouts which get applied as
 * appropriate JDBC statement timeouts. To support the latter, application code
 * must either use {@link cn.taketoday.jdbc.core.JdbcTemplate}, call
 * {@link DataSourceUtils#applyTransactionTimeout} for each created JDBC
 * {@code Statement}, or go through a {@link TransactionAwareDataSourceProxy}
 * which will create timeout-aware JDBC {@code Connection}s and {@code Statement}s
 * automatically.
 *
 * <p>Consider defining a {@link LazyConnectionDataSourceProxy} for your target
 * {@code DataSource}, pointing both this transaction manager and your DAOs to it.
 * This will lead to optimized handling of "empty" transactions, i.e. of transactions
 * without any JDBC statements executed. A {@code LazyConnectionDataSourceProxy} will
 * not fetch an actual JDBC {@code Connection} from the target {@code DataSource}
 * until a {@code Statement} gets executed, lazily applying the specified transaction
 * settings to the target {@code Connection}.
 *
 * <p>This transaction manager supports nested transactions via the JDBC 3.0
 * {@link java.sql.Savepoint} mechanism. The
 * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
 * to "true", since nested transactions will work without restrictions on JDBC
 * drivers that support savepoints (such as the Oracle JDBC driver).
 *
 * <p>This transaction manager can be used as a replacement for the
 * {@link cn.taketoday.transaction.jta.JtaTransactionManager} in the single
 * resource case, as it does not require a container that supports JTA, typically
 * in combination with a locally defined JDBC {@code DataSource} (e.g. a Hikari
 * connection pool). Switching between this local strategy and a JTA environment
 * is just a matter of configuration!
 *
 * <p>this transaction manager triggers flush callbacks on registered
 * transaction synchronizations (if synchronization is generally active), assuming
 * resources operating on the underlying JDBC {@code Connection}. This allows for
 * setup analogous to {@code JtaTransactionManager}, in particular with respect to
 * lazily registered ORM resources (e.g. a Hibernate {@code Session}).
 *
 * <p>{@link cn.taketoday.jdbc.support.JdbcTransactionManager}
 * is available as an extended subclass which includes commit/rollback exception
 * translation, aligned with {@link cn.taketoday.jdbc.core.JdbcTemplate}.</b>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setNestedTransactionAllowed
 * @see java.sql.Savepoint
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#getConnection(javax.sql.DataSource)
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#releaseConnection
 * @see cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy
 * @see cn.taketoday.jdbc.datasource.LazyConnectionDataSourceProxy
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @see cn.taketoday.jdbc.support.JdbcTransactionManager
 * @since 4.0 2021/12/10 21:05
 */
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, InitializingBean {
  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private DataSource dataSource;

  private boolean enforceReadOnly = false;

  /**
   * Create a new {@code DataSourceTransactionManager} instance.
   * A {@code DataSource} has to be set to be able to use it.
   *
   * @see #setDataSource
   */
  public DataSourceTransactionManager() {
    setNestedTransactionAllowed(true);
  }

  /**
   * Create a new {@code DataSourceTransactionManager} instance.
   *
   * @param dataSource the JDBC DataSource to manage transactions for
   */
  public DataSourceTransactionManager(DataSource dataSource) {
    this();
    setDataSource(dataSource);
    afterPropertiesSet();
  }

  /**
   * Set the JDBC DataSource that this instance should manage transactions for.
   * <p>This will typically be a locally defined DataSource, for example an
   * Apache Commons DBCP connection pool. Alternatively, you can also drive
   * transactions for a non-XA J2EE DataSource fetched from JNDI. For an XA
   * DataSource, use JtaTransactionManager.
   * <p>The DataSource specified here should be the target DataSource to manage
   * transactions for, not a TransactionAwareDataSourceProxy. Only data access
   * code may work with TransactionAwareDataSourceProxy, while the transaction
   * manager needs to work on the underlying target DataSource. If there's
   * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
   * unwrapped to extract its target DataSource.
   * <p><b>The DataSource passed in here needs to return independent Connections.</b>
   * The Connections may come from a pool (the typical case), but the DataSource
   * must not return thread-scoped / request-scoped Connections or the like.
   *
   * @see TransactionAwareDataSourceProxy
   * @see cn.taketoday.transaction.jta.JtaTransactionManager
   */
  public void setDataSource(@Nullable DataSource dataSource) {
    if (dataSource instanceof TransactionAwareDataSourceProxy) {
      // If we got a TransactionAwareDataSourceProxy, we need to perform transactions
      // for its underlying target DataSource, else data access code won't see
      // properly exposed transactions (i.e. transactions for the target DataSource).
      this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
    }
    else {
      this.dataSource = dataSource;
    }
  }

  /**
   * Return the JDBC DataSource that this instance manages transactions for.
   */
  @Nullable
  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Obtain the DataSource for actual use.
   *
   * @return the DataSource (never {@code null})
   * @throws IllegalStateException in case of no DataSource set
   * @since 4.0
   */
  protected DataSource obtainDataSource() {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
  }

  /**
   * Specify whether to enforce the read-only nature of a transaction
   * (as indicated by {@link TransactionDefinition#isReadOnly()}
   * through an explicit statement on the transactional connection:
   * "SET TRANSACTION READ ONLY" as understood by Oracle, MySQL and Postgres.
   * <p>The exact treatment, including any SQL statement executed on the connection,
   * can be customized through {@link #prepareTransactionalConnection}.
   * <p>This mode of read-only handling goes beyond the {@link Connection#setReadOnly}
   * hint that Framework applies by default. In contrast to that standard JDBC hint,
   * "SET TRANSACTION READ ONLY" enforces an isolation-level-like connection mode
   * where data manipulation statements are strictly disallowed. Also, on Oracle,
   * this read-only mode provides read consistency for the entire transaction.
   * <p>Note that older Oracle JDBC drivers (9i, 10g) used to enforce this read-only
   * mode even for {@code Connection.setReadOnly(true}. However, with recent drivers,
   * this strong enforcement needs to be applied explicitly, e.g. through this flag.
   *
   * @see #prepareTransactionalConnection
   * @since 4.0
   */
  public void setEnforceReadOnly(boolean enforceReadOnly) {
    this.enforceReadOnly = enforceReadOnly;
  }

  /**
   * Return whether to enforce the read-only nature of a transaction
   * through an explicit statement on the transactional connection.
   *
   * @see #setEnforceReadOnly
   * @since 4.0
   */
  public boolean isEnforceReadOnly() {
    return this.enforceReadOnly;
  }

  @Override
  public void afterPropertiesSet() {
    if (getDataSource() == null) {
      throw new IllegalArgumentException("Property 'dataSource' is required");
    }
  }

  @Override
  public Object getResourceFactory() {
    return obtainDataSource();
  }

  @Override
  protected Object doGetTransaction() {
    DataSourceTransactionObject txObject = new DataSourceTransactionObject();
    txObject.setSavepointAllowed(isNestedTransactionAllowed());
    ConnectionHolder conHolder = TransactionSynchronizationManager.getResource(obtainDataSource());
    txObject.setConnectionHolder(conHolder, false);
    return txObject;
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    Connection con = null;

    try {
      if (!txObject.hasConnectionHolder() || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
        Connection newCon = obtainDataSource().getConnection();
        if (logger.isDebugEnabled()) {
          logger.debug("Acquired Connection [{}] for JDBC transaction", newCon);
        }
        txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
      }

      ConnectionHolder connectionHolder = txObject.getConnectionHolder();
      connectionHolder.setSynchronizedWithTransaction(true);
      con = connectionHolder.getConnection();

      Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
      txObject.setPreviousIsolationLevel(previousIsolationLevel);
      txObject.setReadOnly(definition.isReadOnly());

      // Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
      // so we don't want to do it unnecessarily (for example if we've explicitly
      // configured the connection pool to set it already).
      if (con.getAutoCommit()) {
        txObject.setMustRestoreAutoCommit(true);
        if (logger.isDebugEnabled()) {
          logger.debug("Switching JDBC Connection [{}] to manual commit", con);
        }
        con.setAutoCommit(false);
      }

      prepareTransactionalConnection(con, definition);
      connectionHolder.setTransactionActive(true);

      int timeout = determineTimeout(definition);
      if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
        connectionHolder.setTimeoutInSeconds(timeout);
      }

      // Bind the connection holder to the thread.
      if (txObject.isNewConnectionHolder()) {
        TransactionSynchronizationManager.bindResource(obtainDataSource(), connectionHolder);
      }
    }
    catch (Throwable ex) {
      if (txObject.isNewConnectionHolder()) {
        DataSourceUtils.releaseConnection(con, obtainDataSource());
        txObject.setConnectionHolder(null, false);
      }
      throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
    }
  }

  @Override
  protected Object doSuspend(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    txObject.setConnectionHolder(null);
    return TransactionSynchronizationManager.unbindResource(obtainDataSource());
  }

  @Override
  protected void doResume(@Nullable Object transaction, Object suspendedResources) {
    TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    Connection con = txObject.getConnectionHolder().getConnection();
    if (status.isDebug()) {
      logger.debug("Committing JDBC transaction on Connection [{}]", con);
    }
    try {
      con.commit();
    }
    catch (SQLException ex) {
      throw translateException("JDBC commit", ex);
    }
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    Connection con = txObject.getConnectionHolder().getConnection();
    if (status.isDebug()) {
      logger.debug("Rolling back JDBC transaction on Connection [{}]", con);
    }
    try {
      con.rollback();
    }
    catch (SQLException ex) {
      throw translateException("JDBC rollback", ex);
    }
  }

  @Override
  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Setting JDBC transaction [{}] rollback-only", txObject.getConnectionHolder().getConnection());
    }
    txObject.setRollbackOnly();
  }

  @Override
  protected void doCleanupAfterCompletion(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

    // Remove the connection holder from the thread, if exposed.
    if (txObject.isNewConnectionHolder()) {
      TransactionSynchronizationManager.unbindResource(obtainDataSource());
    }

    // Reset connection.
    Connection con = txObject.getConnectionHolder().getConnection();
    try {
      if (txObject.isMustRestoreAutoCommit()) {
        con.setAutoCommit(true);
      }
      DataSourceUtils.resetConnectionAfterTransaction(
              con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
    }
    catch (Throwable ex) {
      logger.debug("Could not reset JDBC Connection after transaction", ex);
    }

    if (txObject.isNewConnectionHolder()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Releasing JDBC Connection [{}] after transaction", con);
      }
      DataSourceUtils.releaseConnection(con, this.dataSource);
    }

    txObject.getConnectionHolder().clear();
  }

  /**
   * Prepare the transactional {@code Connection} right after transaction begin.
   * <p>The default implementation executes a "SET TRANSACTION READ ONLY" statement
   * if the {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to {@code true}
   * and the transaction definition indicates a read-only transaction.
   * <p>The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
   * and may work with other databases as well. If you'd like to adapt this treatment,
   * override this method accordingly.
   *
   * @param con the transactional JDBC Connection
   * @param definition the current transaction definition
   * @throws SQLException if thrown by JDBC API
   * @see #setEnforceReadOnly
   * @since 4.0
   */
  protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
          throws SQLException {

    if (isEnforceReadOnly() && definition.isReadOnly()) {
      try (Statement stmt = con.createStatement()) {
        stmt.executeUpdate("SET TRANSACTION READ ONLY");
      }
    }
  }

  /**
   * Translate the given JDBC commit/rollback exception to a common Framework
   * exception to propagate from the {@link #commit}/{@link #rollback} call.
   * <p>The default implementation throws a {@link TransactionSystemException}.
   * Subclasses may specifically identify concurrency failures etc.
   *
   * @param task the task description (commit or rollback)
   * @param ex the SQLException thrown from commit/rollback
   * @return the translated exception to throw, either a
   * {@link cn.taketoday.dao.DataAccessException} or a
   * {@link cn.taketoday.transaction.TransactionException}
   */
  protected RuntimeException translateException(String task, SQLException ex) {
    return new TransactionSystemException(task + " failed", ex);
  }

  /**
   * DataSource transaction object, representing a ConnectionHolder.
   * Used as transaction object by DataSourceTransactionManager.
   */
  private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {

    private boolean newConnectionHolder;

    private boolean mustRestoreAutoCommit;

    public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
      super.setConnectionHolder(connectionHolder);
      this.newConnectionHolder = newConnectionHolder;
    }

    public boolean isNewConnectionHolder() {
      return this.newConnectionHolder;
    }

    public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
      this.mustRestoreAutoCommit = mustRestoreAutoCommit;
    }

    public boolean isMustRestoreAutoCommit() {
      return this.mustRestoreAutoCommit;
    }

    public void setRollbackOnly() {
      getConnectionHolder().setRollbackOnly();
    }

    @Override
    public boolean isRollbackOnly() {
      return getConnectionHolder().isRollbackOnly();
    }

    @Override
    public void flush() {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationUtils.triggerFlush();
      }
    }
  }

}
