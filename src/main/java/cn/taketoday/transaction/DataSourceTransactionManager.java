/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.beans.InitializingBean;
import cn.taketoday.core.Assert;
import cn.taketoday.jdbc.utils.DataSourceUtils;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;

/**
 * From Spring DataSourceTransactionManager
 *
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2018-11-13 19:05
 */
public class DataSourceTransactionManager
        extends AbstractTransactionManager implements ResourceTransactionManager, InitializingBean {
  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(DataSourceTransactionManager.class);

  private DataSource dataSource;

  private boolean enforceReadOnly = false;

  public DataSourceTransactionManager() {
    setNestedTransactionAllowed(true);
  }

  public DataSourceTransactionManager(DataSource dataSource) {
    this();
    setDataSource(dataSource);
    afterPropertiesSet();
  }

  /**
   * Obtain the DataSource for actual use.
   *
   * @return the DataSource (never {@code null})
   *
   * @throws NullPointerException
   *         in case of no DataSource set
   */
  protected DataSource obtainDataSource() {
    final DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
  }

  /**
   * Return the JDBC DataSource that this instance manages transactions for.
   */
  public DataSource getDataSource() {
    return this.dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Specify whether to enforce the read-only nature of a transaction (as
   * indicated by {@link TransactionDefinition#isReadOnly()} through an explicit
   * statement on the transactional connection: "SET TRANSACTION READ ONLY" as
   * understood by Oracle, MySQL and Postgres.
   * <p>
   * The exact treatment, including any SQL statement executed on the connection,
   * can be customized through through {@link #prepareTransactionalConnection}.
   * <p>
   * This mode of read-only handling goes beyond the
   * {@link Connection#setReadOnly} hint that applies by default. In
   * contrast to that standard JDBC hint, "SET TRANSACTION READ ONLY" enforces an
   * isolation-level-like connection mode where data manipulation statements are
   * strictly disallowed. Also, on Oracle, this read-only mode provides read
   * consistency for the entire transaction.
   * <p>
   * Note that older Oracle JDBC drivers (9i, 10g) used to enforce this read-only
   * mode even for {@code Connection.setReadOnly(true}. However, with recent
   * drivers, this strong enforcement needs to be applied explicitly, e.g. through
   * this flag.
   *
   * @see #prepareTransactionalConnection
   */
  public void setEnforceReadOnly(boolean enforceReadOnly) {
    this.enforceReadOnly = enforceReadOnly;
  }

  /**
   * Return whether to enforce the read-only nature of a transaction through an
   * explicit statement on the transactional connection.
   *
   * @see #setEnforceReadOnly
   */
  public boolean isEnforceReadOnly() {
    return this.enforceReadOnly;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(getDataSource() != null, "dataSource is required");
  }

  @Override
  public Object getResourceFactory() {
    return dataSource;
  }

  @Override
  protected Object doGetTransaction() {
    final DataSourceTransactionObject txObject = new DataSourceTransactionObject();
    txObject.setSavepointAllowed(isNestedTransactionAllowed());

    final ConnectionHolder conHolder = (ConnectionHolder) SynchronizationManager.getResource(obtainDataSource());
    txObject.setConnectionHolder(conHolder, false);
    return txObject;
  }

  @Override
  protected boolean isExistingTransaction(final Object transaction) {
    final ConnectionHolder conHolder = ((DataSourceTransactionObject) transaction).getConnectionHolder();
    return (conHolder != null && conHolder.isTransactionActive());
  }

  /**
   * This implementation sets the isolation level but ignores the timeout.
   */
  @Override
  protected void doBegin(
          final SynchronizationMetaData metaData, final Object transaction, final TransactionDefinition definition) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    Connection con = null;

    try {
      ConnectionHolder connectionHolder = txObject.getConnectionHolder();
      if (connectionHolder == null || connectionHolder.isSynchronizedWithTransaction()) {
        Connection newCon = obtainDataSource().getConnection();
        if (log.isDebugEnabled()) {
          log.debug("Acquired Connection [{}] for JDBC transaction", newCon);
        }
        txObject.setConnectionHolder(connectionHolder = new ConnectionHolder(newCon), true);
      }

      connectionHolder.setSynchronizedWithTransaction(true);
      con = connectionHolder.getConnection();

      final Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
      txObject.setPreviousIsolationLevel(previousIsolationLevel);

      // Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
      // so we don't want to do it unnecessarily (for example if we've explicitly configured the connection pool to set it already).
      if (con.getAutoCommit()) {
        txObject.setMustRestoreAutoCommit(true);
        if (log.isDebugEnabled()) {
          log.debug("Switching JDBC Connection [{}] to manual commit", con);
        }
        con.setAutoCommit(false);
      }

      prepareTransactionalConnection(con, definition);
      connectionHolder.setTransactionActive(true);

      applyTimeout(definition, connectionHolder);

      // Bind the connection holder to the thread.
      if (txObject.isNewConnectionHolder()) {
        metaData.bindResource(obtainDataSource(), connectionHolder);
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

  protected void applyTimeout(final TransactionDefinition definition, final ConnectionHolder connectionHolder) {
    final int timeout = determineTimeout(definition);
    if (timeout < TransactionDefinition.TIMEOUT_DEFAULT) {
      throw new InvalidTimeoutException("Invalid transaction timeout: ", timeout);
    }
    if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
      connectionHolder.setTimeoutInSeconds(timeout);
    }
  }

  @Override
  protected Object doSuspend(final SynchronizationMetaData metaData, Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    txObject.setConnectionHolder(null);
    return metaData.unbindResource(obtainDataSource());
  }

  @Override
  protected void doResume(final SynchronizationMetaData metaData, Object transaction, Object suspendedResources) {
    metaData.bindResource(obtainDataSource(), suspendedResources);
  }

  @Override
  protected void doCommit(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {
    final DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    final Connection con = txObject.getConnectionHolder().getConnection();
    if (log.isDebugEnabled()) {
      log.debug("Committing JDBC transaction on Connection [{}]", con);
    }
    try {
      con.commit();
    }
    catch (SQLException ex) {
      throw new TransactionSystemException("Could not commit JDBC transaction", ex);
    }
  }

  @Override
  protected void doRollback(final SynchronizationMetaData metaData, final DefaultTransactionStatus status) {
    final DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    final Connection con = txObject.getConnectionHolder().getConnection();

    if (log.isDebugEnabled()) {
      log.debug("Rolling back JDBC transaction on Connection [{}]", con);
    }

    try {
      con.rollback();
    }
    catch (SQLException ex) {
      throw new TransactionSystemException("Could not roll back JDBC transaction With msg:[" + ex.getMessage() + "]", ex);
    }
  }

  @Override
  protected void doSetRollbackOnly(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {
    final DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();

    if (log.isDebugEnabled()) {
      log.debug("Setting JDBC transaction [{}] rollback-only", txObject.getConnectionHolder().getConnection());
    }

    txObject.setRollbackOnly();
  }

  @Override
  protected void doCleanupAfterCompletion(final SynchronizationMetaData metaData, Object transaction) {

    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

    // Remove the connection holder from the thread, if exposed.
    if (txObject.isNewConnectionHolder()) {
      metaData.unbindResource(obtainDataSource());
    }

    // Reset connection.
    Connection con = txObject.getConnectionHolder().getConnection();
    try {

      if (txObject.isMustRestoreAutoCommit()) {
        con.setAutoCommit(true);
      }
      DataSourceUtils.resetConnectionAfterTransaction(con, txObject.getPreviousIsolationLevel());
    }
    catch (Throwable ex) {
      log.error("Could not reset JDBC Connection after transaction", ex);
    }

    if (txObject.isNewConnectionHolder()) {
      if (log.isDebugEnabled()) {
        log.debug("Releasing JDBC Connection [{}] after transaction", con);
      }
      DataSourceUtils.releaseConnection(con, obtainDataSource());
    }
    txObject.getConnectionHolder().clear();
  }

  /**
   * Prepare the transactional {@code Connection} right after transaction begin.
   * <p>
   * The default implementation executes a "SET TRANSACTION READ ONLY" statement
   * if the {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to
   * {@code true} and the transaction definition indicates a read-only
   * transaction.
   * <p>
   * The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
   * and may work with other databases as well. If you'd like to adapt this
   * Statement, override this method accordingly.
   *
   * @param con
   *         the transactional JDBC Connection
   * @param definition
   *         the current transaction definition
   *
   * @throws SQLException
   *         if thrown by JDBC API
   * @see #setEnforceReadOnly
   */
  protected void prepareTransactionalConnection(
          final Connection con, final TransactionDefinition definition) throws SQLException {
    if (isEnforceReadOnly() && definition.isReadOnly()) {
      try (Statement stmt = con.createStatement()) {
        stmt.executeUpdate("SET TRANSACTION READ ONLY");
      }
    }
  }

  /**
   * DataSource transaction object, representing a ConnectionHolder. Used as
   * transaction object by DataSourceTransactionManager.
   */
  private static class DataSourceTransactionObject extends JdbcTransactionObject {

    private boolean newConnectionHolder;
    private boolean mustRestoreAutoCommit;

    public void setConnectionHolder(ConnectionHolder connectionHolder, boolean newConnectionHolder) {
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
      final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();
      if (metaData.isActive()) {
        metaData.triggerFlush();
      }
    }
  }

}
