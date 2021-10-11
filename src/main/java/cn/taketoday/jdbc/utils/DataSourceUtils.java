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
package cn.taketoday.jdbc.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import cn.taketoday.lang.Assert;
import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.ConnectionHolder;
import cn.taketoday.transaction.SynchronizationManager;
import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionSynchronization;

/**
 * @author TODAY <br>
 * 2018-11-06 20:37
 */
public abstract class DataSourceUtils {
  private static final Logger log = LoggerFactory.getLogger(DataSourceUtils.class);

  /**
   * Get jdbc Connection from {@link DataSource}
   *
   * @param dataSource
   *         the DataSource to obtain Connections from
   *
   * @return a JDBC Connection from the given DataSource
   *
   * @see #releaseConnection
   */
  public static Connection getConnection(DataSource dataSource) {
    return getConnection(SynchronizationManager.getMetaData(), dataSource);
  }

  /**
   * Get jdbc Connection from {@link DataSource}
   *
   * @param dataSource
   *         the DataSource to obtain Connections from
   *
   * @return a JDBC Connection from the given DataSource
   *
   * @see #releaseConnection
   */
  public static Connection getConnection(
          final SynchronizationMetaData metaData, final DataSource dataSource) {
    try {
      return doGetConnection(metaData, dataSource);
    }
    catch (SQLException ex) {
      throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
    }
    catch (IllegalStateException ex) {
      throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: " + ex.getMessage(), ex);
    }
  }

  public static Connection doGetConnection(final DataSource dataSource) throws SQLException {
    return doGetConnection(SynchronizationManager.getMetaData(), dataSource);
  }

  public static Connection doGetConnection(final SynchronizationMetaData metaData,
                                           final DataSource dataSource) throws SQLException //
  {
    Assert.notNull(dataSource, "No DataSource specified");

    ConnectionHolder conHolder = null;

    final Object resource = metaData.getResource(dataSource);
    if (resource instanceof ConnectionHolder) {
      conHolder = (ConnectionHolder) resource;

      if (conHolder.isSynchronizedWithTransaction()) {
        if (!conHolder.hasConnection()) {
          if (log.isDebugEnabled()) {
            log.debug("Fetching resumed JDBC Connection from DataSource");
          }
          conHolder.setConnection(fetchConnection(dataSource));
        }
        return conHolder.getConnection();
      }
      if (conHolder.hasConnection()) {
        return conHolder.getConnection();
      }
    }
    // Else we either got no holder or an empty thread-bound holder here.
    if (log.isDebugEnabled()) {
      log.debug("Fetching JDBC Connection from DataSource");
    }

    final Connection ret = fetchConnection(dataSource);

    if (metaData.isActive()) {
      try {
        // Use same Connection for further JDBC actions within the transaction.
        // Thread-bound object will get removed by synchronization at transaction completion.
        ConnectionHolder holderToUse = conHolder;
        if (holderToUse == null) {
          holderToUse = new ConnectionHolder(ret);
        }
        else {
          holderToUse.setConnection(ret);
        }
        holderToUse.requested();
        metaData.registerSynchronization(new ConnectionSynchronization(holderToUse, dataSource));
        holderToUse.setSynchronizedWithTransaction(true);

        if (holderToUse != conHolder) {
          metaData.bindResource(dataSource, holderToUse);
        }
      }
      catch (RuntimeException ex) {
        releaseConnection(ret, dataSource); // Unexpected exception from external delegation call -> close Connection and rethrow.
        throw ex;
      }
    }
    return ret;
  }

  /**
   * Actually fetch a {@link Connection} from the given {@link DataSource},
   * defensively turning an unexpected {@code null} return value from
   * {@link DataSource#getConnection()} into an {@link IllegalStateException}.
   *
   * @param dataSource
   *         the DataSource to obtain Connections from
   *
   * @return a JDBC Connection from the given DataSource (never {@code null})
   *
   * @throws SQLException
   *         if thrown by JDBC methods ,if a database access error occurs
   * @throws java.sql.SQLTimeoutException
   *         when the driver has determined that the
   * @see DataSource#getConnection()
   */
  public static Connection fetchConnection(DataSource dataSource) throws SQLException {
    return dataSource.getConnection();
  }

  /**
   * Prepare the given Connection with the given transaction semantics.
   *
   * @param con
   *         the Connection to prepare
   * @param definition
   *         the transaction definition to apply
   *
   * @return the previous isolation level, if any
   *
   * @throws SQLException
   *         if thrown by JDBC methods
   * @see #resetConnectionAfterTransaction
   */
  public static Integer prepareConnectionForTransaction(
          final Connection con,
          final TransactionDefinition definition) throws SQLException //
  {
    if (definition != null) {
      Assert.notNull(con, "No Connection specified");
      // Set read-only flag.
      if (definition.isReadOnly()) {
        if (log.isDebugEnabled()) {
          log.debug("Setting JDBC Connection [{}] read-only", con);
        }
        con.setReadOnly(true);
      }

      // Apply specific isolation level, if any.
      Integer previousIsolationLevel = null;
      final int isolationLevel = definition.getIsolationLevel();
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
   * Reset the given Connection after a transaction, regarding read-only flag and
   * isolation level.
   *
   * @param con
   *         the Connection to reset
   * @param previousIsolationLevel
   *         the isolation level to restore, if any
   *
   * @see #prepareConnectionForTransaction
   */
  public static void resetConnectionAfterTransaction(Connection con, Integer previousIsolationLevel) {
    Assert.notNull(con, "No Connection specified");

    try {
      // Reset transaction isolation to previous value, if changed for the transaction.
      if (previousIsolationLevel != null) {
        if (log.isDebugEnabled()) {
          log.debug("Resetting isolation level of JDBC Connection [{}] to [{}]", con, previousIsolationLevel);
        }
        con.setTransactionIsolation(previousIsolationLevel);
      }

      // Reset read-only flag.
      if (con.isReadOnly()) {
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
   * @param con
   *         the Connection to check
   * @param dataSource
   *         the DataSource that the Connection was obtained from (may be
   *         {@code null})
   *
   * @return whether the Connection is transactional
   */
  public static boolean isConnectionTransactional(Connection con, DataSource dataSource) {
    if (dataSource == null) {
      return false;
    }
    final ConnectionHolder conHolder = (ConnectionHolder) SynchronizationManager.getResource(dataSource);
    return (conHolder != null && connectionEquals(conHolder, con));
  }

  /**
   * Apply the current transaction timeout, if any, to the given JDBC Statement
   * object.
   *
   * @param stmt
   *         the JDBC Statement object
   * @param dataSource
   *         the DataSource that the Connection was obtained from
   *
   * @throws SQLException
   *         if thrown by JDBC methods
   * @see java.sql.Statement#setQueryTimeout
   */
  public static void applyTransactionTimeout(Statement stmt, DataSource dataSource) throws SQLException {
    applyTimeout(stmt, dataSource, -1);
  }

  /**
   * Apply the specified timeout - overridden by the current transaction timeout,
   * if any - to the given JDBC Statement object.
   *
   * @param stmt
   *         the JDBC Statement object
   * @param dataSource
   *         the DataSource that the Connection was obtained from
   * @param timeout
   *         the timeout to apply (or 0 for no timeout outside of a
   *         transaction)
   *
   * @throws SQLException
   *         if thrown by JDBC methods
   * @see java.sql.Statement#setQueryTimeout
   */
  public static void applyTimeout(Statement stmt, DataSource dataSource, Integer timeout) throws SQLException {
    Assert.notNull(stmt, "No Statement specified");

    ConnectionHolder holder = null;
    if (dataSource != null) {
      holder = (ConnectionHolder) SynchronizationManager.getResource(dataSource);
    }
    if (holder != null && holder.hasTimeout()) {
      // Remaining transaction timeout overrides specified value.
      stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
    }
    else if (timeout != null && timeout >= 0) {
      // No current transaction timeout -> apply specified value.
      stmt.setQueryTimeout(timeout);
    }
  }

  /**
   * Close the given Connection, obtained from the given DataSource, if it is not
   * managed externally (that is, not bound to the thread).
   *
   * @param con
   *         the Connection to close if necessary (if this is {@code null}, the
   *         call will be ignored)
   * @param dataSource
   *         the DataSource that the Connection was obtained from (may be
   *         {@code null})
   *
   * @see #getConnection
   */
  public static void releaseConnection(Connection con, DataSource dataSource) {
    try {
      doReleaseConnection(con, dataSource);
    }
    catch (SQLException ex) {
      log.debug("Could not close JDBC Connection", ex);
    }
    catch (Throwable ex) {
      log.error("Unexpected exception on closing JDBC Connection", ex);
    }
  }

  /**
   * Actually close the given Connection, obtained from the given DataSource. Same
   * as {@link #releaseConnection}, but throwing the original SQLException.
   * <p>
   *
   * @param con
   *         the Connection to close if necessary (if this is {@code null}, the
   *         call will be ignored)
   * @param dataSource
   *         the DataSource that the Connection was obtained from (may be
   *         {@code null})
   *
   * @throws SQLException
   *         if thrown by JDBC methods
   * @see #doGetConnection
   */
  public static void doReleaseConnection(Connection con, DataSource dataSource) throws SQLException {
    if (con != null) {
      if (dataSource != null) {
        final ConnectionHolder conHolder = (ConnectionHolder) SynchronizationManager.getResource(dataSource);
        if (conHolder != null && connectionEquals(conHolder, con)) {
          // It's the transactional Connection: Don't close it.
          conHolder.released();
          return;
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Returning JDBC Connection to DataSource");
      }
      con.close();
    }
  }

  /**
   * Determine whether the given two Connections are equal, asking the target
   * Connection in case of a proxy. Used to detect equality even if the user
   * passed in a raw target Connection while the held one is a proxy.
   *
   * @param conHolder
   *         the ConnectionHolder for the held Connection (potentially a proxy)
   * @param passedInCon
   *         the Connection passed-in by the user (potentially a target
   *         Connection without proxy)
   *
   * @return whether the given Connections are equal
   */
  private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {

    if (conHolder.hasConnection()) {
      final Connection heldCon = conHolder.getConnection();
      // Explicitly check for identity too: for Connection handles that do not
      // implement  "equals" properly, such as the ones Commons DBCP exposes).
      return (heldCon == passedInCon || heldCon.equals(passedInCon));
    }
    return false;
  }

  /**
   * Callback for resource cleanup at the end of a non-native JDBC transaction
   * (e.g. when participating in a JtaTransactionManager transaction).
   */
  private static class ConnectionSynchronization implements TransactionSynchronization {

    private final ConnectionHolder connectionHolder;

    private final DataSource dataSource;

    private boolean holderActive = true;

    public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
      this.connectionHolder = connectionHolder;
      this.dataSource = dataSource;
    }

    @Override
    public void suspend(final SynchronizationMetaData metaData) {
      if (this.holderActive) {
        metaData.unbindResource(this.dataSource);
        final ConnectionHolder connectionHolder = this.connectionHolder;
        if (connectionHolder.hasConnection() && !connectionHolder.isOpen()) {
          // Release Connection on suspend if the application doesn't keep
          // a handle to it anymore. We will fetch a fresh Connection if the
          // application accesses the ConnectionHolder again after resume,
          // assuming that it will participate in the same transaction.
          releaseConnection(connectionHolder.getConnection(), this.dataSource);
          connectionHolder.setConnection(null);
        }
      }
    }

    @Override
    public void resume(final SynchronizationMetaData metaData) {
      if (this.holderActive) {
        metaData.bindResource(this.dataSource, this.connectionHolder);
      }
    }

    @Override
    public void beforeCompletion(final SynchronizationMetaData metaData) {
      // Release Connection early if the holder is not open anymore
      // (that is, not used by another resource like a Hibernate Session
      // that has its own cleanup via transaction synchronization),
      // to avoid issues with strict JTA implementations that expect
      // the close call before transaction completion.
      final ConnectionHolder connectionHolder = this.connectionHolder;
      if (!connectionHolder.isOpen()) {
        metaData.unbindResource(this.dataSource);
        this.holderActive = false;
        if (connectionHolder.hasConnection()) {
          releaseConnection(connectionHolder.getConnection(), this.dataSource);
        }
      }
    }

    @Override
    public void afterCompletion(final SynchronizationMetaData metaData, int status) {
//			log.debug("After Completion with status: [{}]", status);
      // If we haven't closed the Connection in beforeCompletion,
      // close it now. The holder might have been used for other
      // cleanup in the meantime, for example by a Hibernate Session.
      final ConnectionHolder connectionHolder = this.connectionHolder;
      if (this.holderActive) {
        // The thread-bound ConnectionHolder might not be available anymore,
        // since afterCompletion might get called from a different thread.
        metaData.unbindResourceIfPossible(this.dataSource);
        this.holderActive = false;
        if (connectionHolder.hasConnection()) {
          releaseConnection(connectionHolder.getConnection(), this.dataSource);
          // Reset the ConnectionHolder: It might remain bound to the thread.
          connectionHolder.setConnection(null);
        }
      }
      connectionHolder.reset();
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
