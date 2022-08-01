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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * Helper class that provides static methods for obtaining JDBC Connections from
 * a {@link javax.sql.DataSource}. Includes special support for Framework-managed
 * transactional Connections, e.g. managed by {@link DataSourceTransactionManager}
 * or {@link cn.taketoday.transaction.jta.JtaTransactionManager}.
 *
 * <p>Used internally by Framework's {@link cn.taketoday.jdbc.core.JdbcTemplate},
 * Framework's JDBC operation objects and the JDBC {@link DataSourceTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getConnection
 * @see #releaseConnection
 * @see DataSourceTransactionManager
 * @see cn.taketoday.transaction.jta.JtaTransactionManager
 * @see cn.taketoday.transaction.support.TransactionSynchronizationManager
 * @since 2018-11-06 20:37
 */
public abstract class DataSourceUtils {
  private static final Logger log = LoggerFactory.getLogger(DataSourceUtils.class);
  /**
   * Order value for TransactionSynchronization objects that clean up JDBC Connections.
   */
  public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

  /**
   * Get jdbc Connection from {@link DataSource}
   *
   * @param dataSource the DataSource to obtain Connections from
   * @return a JDBC Connection from the given DataSource
   * @see #releaseConnection
   */
  public static Connection getConnection(DataSource dataSource) {
    return getConnection(TransactionSynchronizationManager.getSynchronizationInfo(), dataSource);
  }

  /**
   * Get jdbc Connection from {@link DataSource}
   *
   * @param dataSource the DataSource to obtain Connections from
   * @return a JDBC Connection from the given DataSource
   * @see #releaseConnection
   */
  public static Connection getConnection(
          SynchronizationInfo metaData, DataSource dataSource) {
    try {
      return doGetConnection(metaData, dataSource);
    }
    catch (SQLException | IllegalStateException ex) {
      throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
    }
  }

  /**
   * Actually obtain a JDBC Connection from the given DataSource.
   * Same as {@link #getConnection}, but throwing the original SQLException.
   * <p>Is aware of a corresponding Connection bound to the current thread, for example
   * when using {@link DataSourceTransactionManager}. Will bind a Connection to the thread
   * if transaction synchronization is active (e.g. if in a JTA transaction).
   * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
   *
   * @param dataSource the DataSource to obtain Connections from
   * @return a JDBC Connection from the given DataSource
   * @throws SQLException if thrown by JDBC methods
   * @see #doReleaseConnection
   */
  public static Connection doGetConnection(DataSource dataSource) throws SQLException {
    return doGetConnection(TransactionSynchronizationManager.getSynchronizationInfo(), dataSource);
  }

  /**
   * Actually obtain a JDBC Connection from the given DataSource.
   * Same as {@link #getConnection}, but throwing the original SQLException.
   * <p>Is aware of a corresponding Connection bound to the current thread, for example
   * when using {@link DataSourceTransactionManager}. Will bind a Connection to the thread
   * if transaction synchronization is active (e.g. if in a JTA transaction).
   * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
   *
   * @param dataSource the DataSource to obtain Connections from
   * @return a JDBC Connection from the given DataSource
   * @throws SQLException if thrown by JDBC methods
   * @see #doReleaseConnection
   */
  public static Connection doGetConnection(
          SynchronizationInfo info, DataSource dataSource) throws SQLException {
    Assert.notNull(dataSource, "No DataSource specified");

    ConnectionHolder conHolder = (ConnectionHolder) info.getResource(dataSource);
    if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
      conHolder.requested();
      if (!conHolder.hasConnection()) {
        if (log.isDebugEnabled()) {
          log.debug("Fetching resumed JDBC Connection from DataSource");
        }
        conHolder.setConnection(fetchConnection(dataSource));
      }
      return conHolder.getConnection();
    }
    // Else we either got no holder or an empty thread-bound holder here.

    if (log.isDebugEnabled()) {
      log.debug("Fetching JDBC Connection from DataSource");
    }
    Connection con = fetchConnection(dataSource);

    if (info.isSynchronizationActive()) {
      try {
        // Use same Connection for further JDBC actions within the transaction.
        // Thread-bound object will get removed by synchronization at transaction completion.
        ConnectionHolder holderToUse = conHolder;
        if (holderToUse == null) {
          holderToUse = new ConnectionHolder(con);
        }
        else {
          holderToUse.setConnection(con);
        }
        holderToUse.requested();
        info.registerSynchronization(new ConnectionSynchronization(holderToUse, dataSource));
        holderToUse.setSynchronizedWithTransaction(true);

        if (holderToUse != conHolder) {
          info.bindResource(dataSource, holderToUse);
        }
      }
      catch (RuntimeException ex) {
        // Unexpected exception from external delegation call -> close Connection and rethrow.
        releaseConnection(con, dataSource);
        throw ex;
      }
    }

    return con;
  }

  /**
   * Actually fetch a {@link Connection} from the given {@link DataSource},
   * defensively turning an unexpected {@code null} return value from
   * {@link DataSource#getConnection()} into an {@link IllegalStateException}.
   *
   * @param dataSource the DataSource to obtain Connections from
   * @return a JDBC Connection from the given DataSource (never {@code null})
   * @throws SQLException if thrown by JDBC methods
   * @throws IllegalStateException if the DataSource returned a null value
   * @see DataSource#getConnection()
   */
  public static Connection fetchConnection(DataSource dataSource) throws SQLException {
    Connection con = dataSource.getConnection();
    if (con == null) {
      throw new IllegalStateException("DataSource returned null from getConnection(): " + dataSource);
    }
    return con;
  }

  /**
   * Prepare the given Connection with the given transaction semantics.
   *
   * @param con the Connection to prepare
   * @param definition the transaction definition to apply
   * @return the previous isolation level, if any
   * @throws SQLException if thrown by JDBC methods
   * @see #resetConnectionAfterTransaction
   * @see Connection#setTransactionIsolation
   * @see Connection#setReadOnly
   */
  @Nullable
  public static Integer prepareConnectionForTransaction(
          Connection con, @Nullable TransactionDefinition definition) throws SQLException {
    if (definition != null) {
      Assert.notNull(con, "No Connection specified");
      // Set read-only flag.
      if (definition.isReadOnly()) {
        if (log.isDebugEnabled()) {
          log.debug("Setting JDBC Connection [{}] read-only", con);
        }
        try {
          con.setReadOnly(true);
        }
        catch (SQLException | RuntimeException ex) {
          Throwable exToCheck = ex;
          while (exToCheck != null) {
            if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
              // Assume it's a connection timeout that would otherwise get lost: e.g. from JDBC 4.0
              throw ex;
            }
            exToCheck = exToCheck.getCause();
          }
          // "read-only not supported" SQLException -> ignore, it's just a hint anyway
          log.debug("Could not set JDBC Connection read-only", ex);
        }
      }

      // Apply specific isolation level, if any.
      Integer previousIsolationLevel = null;
      int isolationLevel = definition.getIsolationLevel();
      if (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
        if (log.isDebugEnabled()) {
          log.debug("Changing isolation level of JDBC Connection [{}] to {}", con, isolationLevel);
        }
        int currentIsolation = con.getTransactionIsolation();
        if (currentIsolation != isolationLevel) {
          previousIsolationLevel = currentIsolation;
          con.setTransactionIsolation(isolationLevel);
        }
      }
      return previousIsolationLevel;
    }
    return null;
  }

  /**
   * Reset the given Connection after a transaction,
   * regarding read-only flag and isolation level.
   *
   * @param con the Connection to reset
   * @param previousIsolationLevel the isolation level to restore, if any
   * @param resetReadOnly whether to reset the connection's read-only flag
   * @see #prepareConnectionForTransaction
   * @see Connection#setTransactionIsolation
   * @see Connection#setReadOnly
   * @since 4.0
   */
  public static void resetConnectionAfterTransaction(
          Connection con, @Nullable Integer previousIsolationLevel, boolean resetReadOnly) {
    Assert.notNull(con, "No Connection specified");
    try {
      // Reset transaction isolation to previous value, if changed for the transaction.
      if (previousIsolationLevel != null) {
        if (log.isDebugEnabled()) {
          log.debug("Resetting isolation level of JDBC Connection [{}] to {}", con, previousIsolationLevel);
        }
        con.setTransactionIsolation(previousIsolationLevel);
      }

      // Reset read-only flag if we originally switched it to true on transaction begin.
      if (resetReadOnly) {
        if (log.isDebugEnabled()) {
          log.debug("Resetting read-only flag of JDBC Connection [{}]", con);
        }
        con.setReadOnly(false);
      }
    }
    catch (Throwable ex) {
      log.debug("Could not reset JDBC Connection after transaction", ex);
    }
  }

  /**
   * Determine whether the given JDBC Connection is transactional, that is, bound
   * to the current thread.
   *
   * @param con the Connection to check
   * @param dataSource the DataSource that the Connection was obtained from (may be
   * {@code null})
   * @return whether the Connection is transactional
   */
  public static boolean isConnectionTransactional(Connection con, @Nullable DataSource dataSource) {
    if (dataSource == null) {
      return false;
    }
    ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    return (conHolder != null && connectionEquals(conHolder, con));
  }

  /**
   * Apply the current transaction timeout, if any, to the given JDBC Statement
   * object.
   *
   * @param stmt the JDBC Statement object
   * @param dataSource the DataSource that the Connection was obtained from
   * @throws SQLException if thrown by JDBC methods
   * @see java.sql.Statement#setQueryTimeout
   */
  public static void applyTransactionTimeout(Statement stmt, @Nullable DataSource dataSource) throws SQLException {
    applyTimeout(stmt, dataSource, -1);
  }

  /**
   * Apply the specified timeout - overridden by the current transaction timeout,
   * if any - to the given JDBC Statement object.
   *
   * @param stmt the JDBC Statement object
   * @param dataSource the DataSource that the Connection was obtained from
   * @param timeout the timeout to apply (or 0 for no timeout outside of a
   * transaction)
   * @throws SQLException if thrown by JDBC methods
   * @see java.sql.Statement#setQueryTimeout
   */
  public static void applyTimeout(
          Statement stmt, @Nullable DataSource dataSource, int timeout) throws SQLException {
    Assert.notNull(stmt, "No Statement specified");
    ConnectionHolder holder = null;
    if (dataSource != null) {
      holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    }
    if (holder != null && holder.hasTimeout()) {
      // Remaining transaction timeout overrides specified value.
      stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
    }
    else if (timeout >= 0) {
      // No current transaction timeout -> apply specified value.
      stmt.setQueryTimeout(timeout);
    }

  }

  /**
   * Close the given Connection, obtained from the given DataSource,
   * if it is not managed externally (that is, not bound to the thread).
   *
   * @param con the Connection to close if necessary
   * (if this is {@code null}, the call will be ignored)
   * @param dataSource the DataSource that the Connection was obtained from
   * (may be {@code null})
   * @see #getConnection
   */
  public static void releaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) {
    try {
      doReleaseConnection(con, dataSource);
    }
    catch (SQLException ex) {
      log.debug("Could not close JDBC Connection", ex);
    }
    catch (Throwable ex) {
      log.debug("Unexpected exception on closing JDBC Connection", ex);
    }
  }

  /**
   * Actually close the given Connection, obtained from the given DataSource. Same
   * as {@link #releaseConnection}, but throwing the original SQLException.
   * <p>
   *
   * @param con the Connection to close if necessary (if this is {@code null}, the
   * call will be ignored)
   * @param dataSource the DataSource that the Connection was obtained from (may be
   * {@code null})
   * @throws SQLException if thrown by JDBC methods
   * @see #doGetConnection
   */
  public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) throws SQLException {
    if (con != null) {
      if (dataSource != null) {
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder != null && connectionEquals(conHolder, con)) {
          // It's the transactional Connection: Don't close it.
          conHolder.released();
          return;
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Returning JDBC Connection to DataSource");
      }
      doCloseConnection(con, dataSource);
    }
  }

  /**
   * Close the Connection, unless a {@link SmartDataSource} doesn't want us to.
   *
   * @param con the Connection to close if necessary
   * @param dataSource the DataSource that the Connection was obtained from
   * @throws SQLException if thrown by JDBC methods
   * @see Connection#close()
   * @see SmartDataSource#shouldClose(Connection)
   */
  public static void doCloseConnection(Connection con, @Nullable DataSource dataSource) throws SQLException {
    if (!(dataSource instanceof SmartDataSource smartDataSource) || smartDataSource.shouldClose(con)) {
      con.close();
    }
  }

  /**
   * Determine whether the given two Connections are equal, asking the target
   * Connection in case of a proxy. Used to detect equality even if the user
   * passed in a raw target Connection while the held one is a proxy.
   *
   * @param conHolder the ConnectionHolder for the held Connection (potentially a proxy)
   * @param passedInCon the Connection passed-in by the user (potentially a target
   * Connection without proxy)
   * @return whether the given Connections are equal
   */
  private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
    if (conHolder.hasConnection()) {
      Connection heldCon = conHolder.getConnection();
      // Explicitly check for identity too: for Connection handles that do not
      // implement  "equals" properly, such as the ones Commons DBCP exposes).
      return (heldCon == passedInCon || heldCon.equals(passedInCon));
    }
    return false;
  }

  /**
   * Return the innermost target Connection of the given Connection. If the given
   * Connection is a proxy, it will be unwrapped until a non-proxy Connection is
   * found. Otherwise, the passed-in Connection will be returned as-is.
   *
   * @param con the Connection proxy to unwrap
   * @return the innermost target Connection, or the passed-in one if no proxy
   * @see ConnectionProxy#getTargetConnection()
   */
  public static Connection getTargetConnection(Connection con) {
    Connection conToUse = con;
    while (conToUse instanceof ConnectionProxy connectionProxy) {
      conToUse = connectionProxy.getTargetConnection();
    }
    return conToUse;
  }

  /**
   * Determine the connection synchronization order to use for the given
   * DataSource. Decreased for every level of nesting that a DataSource
   * has, checked through the level of DelegatingDataSource nesting.
   *
   * @param dataSource the DataSource to check
   * @return the connection synchronization order to use
   * @see #CONNECTION_SYNCHRONIZATION_ORDER
   */
  private static int getConnectionSynchronizationOrder(DataSource dataSource) {
    int order = CONNECTION_SYNCHRONIZATION_ORDER;
    DataSource currDs = dataSource;
    while (currDs instanceof DelegatingDataSource delegatingDataSource) {
      order--;
      currDs = delegatingDataSource.getTargetDataSource();
    }
    return order;
  }

  /**
   * Callback for resource cleanup at the end of a non-native JDBC transaction
   * (e.g. when participating in a JtaTransactionManager transaction).
   *
   * @see cn.taketoday.transaction.jta.JtaTransactionManager
   */
  private static class ConnectionSynchronization implements TransactionSynchronization {

    private final ConnectionHolder connectionHolder;

    private final DataSource dataSource;

    private final int order;

    private boolean holderActive = true;

    public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
      this.connectionHolder = connectionHolder;
      this.dataSource = dataSource;
      this.order = getConnectionSynchronizationOrder(dataSource);
    }

    @Override
    public int getOrder() {
      return this.order;
    }

    @Override
    public void suspend() {
      if (this.holderActive) {
        TransactionSynchronizationManager.unbindResource(this.dataSource);
        if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
          // Release Connection on suspend if the application doesn't keep
          // a handle to it anymore. We will fetch a fresh Connection if the
          // application accesses the ConnectionHolder again after resume,
          // assuming that it will participate in the same transaction.
          releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
          this.connectionHolder.setConnection(null);
        }
      }
    }

    @Override
    public void resume() {
      if (this.holderActive) {
        TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
      }
    }

    @Override
    public void beforeCompletion() {
      // Release Connection early if the holder is not open anymore
      // (that is, not used by another resource like a Hibernate Session
      // that has its own cleanup via transaction synchronization),
      // to avoid issues with strict JTA implementations that expect
      // the close call before transaction completion.
      if (!this.connectionHolder.isOpen()) {
        TransactionSynchronizationManager.unbindResource(this.dataSource);
        this.holderActive = false;
        if (this.connectionHolder.hasConnection()) {
          releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
        }
      }
    }

    @Override
    public void afterCompletion(int status) {
      // If we haven't closed the Connection in beforeCompletion,
      // close it now. The holder might have been used for other
      // cleanup in the meantime, for example by a Hibernate Session.
      if (this.holderActive) {
        // The thread-bound ConnectionHolder might not be available anymore,
        // since afterCompletion might get called from a different thread.
        TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
        this.holderActive = false;
        if (this.connectionHolder.hasConnection()) {
          releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
          // Reset the ConnectionHolder: It might remain bound to the thread.
          this.connectionHolder.setConnection(null);
        }
      }
      this.connectionHolder.reset();
    }
  }

  public static DataSource getJndiDatasource(String jndiLookup) {
    InitialContext ctx = null;
    try {
      ctx = new InitialContext();
      return (DataSource) ctx.lookup(jndiLookup);
    }
    catch (NamingException e) {
      throw new RuntimeException(e);
    }
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        }
        catch (Throwable e) {
          log.warn("error closing context", e);
        }
      }
    }
  }
}
