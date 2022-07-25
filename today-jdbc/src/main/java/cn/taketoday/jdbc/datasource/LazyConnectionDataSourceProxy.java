/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.core.Constants;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Proxy for a target DataSource, fetching actual JDBC Connections lazily,
 * i.e. not until first creation of a Statement. Connection initialization
 * properties like auto-commit mode, transaction isolation and read-only mode
 * will be kept and applied to the actual JDBC Connection as soon as an
 * actual Connection is fetched (if ever). Consequently, commit and rollback
 * calls will be ignored if no Statements have been created.
 *
 * <p>This DataSource proxy allows to avoid fetching JDBC Connections from
 * a pool unless actually necessary. JDBC transaction control can happen
 * without fetching a Connection from the pool or communicating with the
 * database; this will be done lazily on first creation of a JDBC Statement.
 *
 * <p><b>If you configure both a LazyConnectionDataSourceProxy and a
 * TransactionAwareDataSourceProxy, make sure that the latter is the outermost
 * DataSource.</b> In such a scenario, data access code will talk to the
 * transaction-aware DataSource, which will in turn work with the
 * LazyConnectionDataSourceProxy.
 *
 * <p>Lazy fetching of physical JDBC Connections is particularly beneficial
 * in a generic transaction demarcation environment. It allows you to demarcate
 * transactions on all methods that could potentially perform data access,
 * without paying a performance penalty if no actual data access happens.
 *
 * <p>This DataSource proxy gives you behavior analogous to JTA and a
 * transactional JNDI DataSource (as provided by the Jakarta EE server), even
 * with a local transaction strategy like DataSourceTransactionManager or
 * HibernateTransactionManager. It does not add value with
 * JtaTransactionManager as transaction strategy.
 *
 * <p>Lazy fetching of JDBC Connections is also recommended for read-only
 * operations with Hibernate, in particular if the chances of resolving the
 * result in the second-level cache are high. This avoids the need to
 * communicate with the database at all for such read-only operations.
 * You will get the same effect with non-transactional reads, but lazy fetching
 * of JDBC Connections allows you to still perform reads in transactions.
 *
 * <p><b>NOTE:</b> This DataSource proxy needs to return wrapped Connections
 * (which implement the {@link ConnectionProxy} interface) in order to handle
 * lazy fetching of an actual JDBC Connection. Use {@link Connection#unwrap}
 * to retrieve the native JDBC Connection.
 *
 * @author Juergen Hoeller
 * @see DataSourceTransactionManager
 * @since 4.0
 */
public class LazyConnectionDataSourceProxy extends DelegatingDataSource {

  /** Constants instance for TransactionDefinition. */
  private static final Constants constants = new Constants(Connection.class);

  private static final Logger logger = LoggerFactory.getLogger(LazyConnectionDataSourceProxy.class);

  @Nullable
  private Boolean defaultAutoCommit;

  @Nullable
  private Integer defaultTransactionIsolation;

  /**
   * Create a new LazyConnectionDataSourceProxy.
   *
   * @see #setTargetDataSource
   */
  public LazyConnectionDataSourceProxy() {
  }

  /**
   * Create a new LazyConnectionDataSourceProxy.
   *
   * @param targetDataSource the target DataSource
   */
  public LazyConnectionDataSourceProxy(DataSource targetDataSource) {
    setTargetDataSource(targetDataSource);
    afterPropertiesSet();
  }

  /**
   * Set the default auto-commit mode to expose when no target Connection
   * has been fetched yet (when the actual JDBC Connection default is not known yet).
   * <p>If not specified, the default gets determined by checking a target
   * Connection on startup. If that check fails, the default will be determined
   * lazily on first access of a Connection.
   *
   * @see Connection#setAutoCommit
   */
  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    this.defaultAutoCommit = defaultAutoCommit;
  }

  /**
   * Set the default transaction isolation level to expose when no target Connection
   * has been fetched yet (when the actual JDBC Connection default is not known yet).
   * <p>This property accepts the int constant value (e.g. 8) as defined in the
   * {@link Connection} interface; it is mainly intended for programmatic
   * use. Consider using the "defaultTransactionIsolationName" property for setting
   * the value by name (e.g. "TRANSACTION_SERIALIZABLE").
   * <p>If not specified, the default gets determined by checking a target
   * Connection on startup. If that check fails, the default will be determined
   * lazily on first access of a Connection.
   *
   * @see #setDefaultTransactionIsolationName
   * @see Connection#setTransactionIsolation
   */
  public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
    this.defaultTransactionIsolation = defaultTransactionIsolation;
  }

  /**
   * Set the default transaction isolation level by the name of the corresponding
   * constant in {@link Connection}, e.g. "TRANSACTION_SERIALIZABLE".
   *
   * @param constantName name of the constant
   * @see #setDefaultTransactionIsolation
   * @see Connection#TRANSACTION_READ_UNCOMMITTED
   * @see Connection#TRANSACTION_READ_COMMITTED
   * @see Connection#TRANSACTION_REPEATABLE_READ
   * @see Connection#TRANSACTION_SERIALIZABLE
   */
  public void setDefaultTransactionIsolationName(String constantName) {
    setDefaultTransactionIsolation(constants.asNumber(constantName).intValue());
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();

    // Determine default auto-commit and transaction isolation
    // via a Connection from the target DataSource, if possible.
    if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
      try {
        try (Connection con = obtainTargetDataSource().getConnection()) {
          checkDefaultConnectionProperties(con);
        }
      }
      catch (SQLException ex) {
        logger.debug("Could not retrieve default auto-commit and transaction isolation settings", ex);
      }
    }
  }

  /**
   * Check the default connection properties (auto-commit, transaction isolation),
   * keeping them to be able to expose them correctly without fetching an actual
   * JDBC Connection from the target DataSource.
   * <p>This will be invoked once on startup, but also for each retrieval of a
   * target Connection. If the check failed on startup (because the database was
   * down), we'll lazily retrieve those settings.
   *
   * @param con the Connection to use for checking
   * @throws SQLException if thrown by Connection methods
   */
  protected synchronized void checkDefaultConnectionProperties(Connection con) throws SQLException {
    if (this.defaultAutoCommit == null) {
      this.defaultAutoCommit = con.getAutoCommit();
    }
    if (this.defaultTransactionIsolation == null) {
      this.defaultTransactionIsolation = con.getTransactionIsolation();
    }
  }

  /**
   * Expose the default auto-commit value.
   */
  @Nullable
  protected Boolean defaultAutoCommit() {
    return this.defaultAutoCommit;
  }

  /**
   * Expose the default transaction isolation value.
   */
  @Nullable
  protected Integer defaultTransactionIsolation() {
    return this.defaultTransactionIsolation;
  }

  /**
   * Return a Connection handle that lazily fetches an actual JDBC Connection
   * when asked for a Statement (or PreparedStatement or CallableStatement).
   * <p>The returned Connection handle implements the ConnectionProxy interface,
   * allowing to retrieve the underlying target Connection.
   *
   * @return a lazy Connection handle
   * @see ConnectionProxy#getTargetConnection()
   */
  @Override
  public Connection getConnection() throws SQLException {
    return (Connection) Proxy.newProxyInstance(
            ConnectionProxy.class.getClassLoader(),
            new Class<?>[] { ConnectionProxy.class },
            new LazyConnectionInvocationHandler());
  }

  /**
   * Return a Connection handle that lazily fetches an actual JDBC Connection
   * when asked for a Statement (or PreparedStatement or CallableStatement).
   * <p>The returned Connection handle implements the ConnectionProxy interface,
   * allowing to retrieve the underlying target Connection.
   *
   * @param username the per-Connection username
   * @param password the per-Connection password
   * @return a lazy Connection handle
   * @see ConnectionProxy#getTargetConnection()
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return (Connection) Proxy.newProxyInstance(
            ConnectionProxy.class.getClassLoader(),
            new Class<?>[] { ConnectionProxy.class },
            new LazyConnectionInvocationHandler(username, password));
  }

  /**
   * Invocation handler that defers fetching an actual JDBC Connection
   * until first creation of a Statement.
   */
  private class LazyConnectionInvocationHandler implements InvocationHandler {

    @Nullable
    private String username;

    @Nullable
    private String password;

    @Nullable
    private Boolean autoCommit;

    @Nullable
    private Integer transactionIsolation;

    private boolean readOnly = false;

    private int holdability = ResultSet.CLOSE_CURSORS_AT_COMMIT;

    private boolean closed = false;

    @Nullable
    private Connection target;

    public LazyConnectionInvocationHandler() {
      this.autoCommit = defaultAutoCommit();
      this.transactionIsolation = defaultTransactionIsolation();
    }

    public LazyConnectionInvocationHandler(String username, String password) {
      this();
      this.username = username;
      this.password = password;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on ConnectionProxy interface coming in...

      switch (method.getName()) {
        case "equals":
          // We must avoid fetching a target Connection for "equals".
          // Only consider equal when proxies are identical.
          return (proxy == args[0]);
        case "hashCode":
          // We must avoid fetching a target Connection for "hashCode",
          // and we must return the same hash code even when the target
          // Connection has been fetched: use hashCode of Connection proxy.
          return System.identityHashCode(proxy);
        case "getTargetConnection":
          // Handle getTargetConnection method: return underlying connection.
          return getTargetConnection(method);
        case "unwrap":
          if (((Class<?>) args[0]).isInstance(proxy)) {
            return proxy;
          }
          break;
        case "isWrapperFor":
          if (((Class<?>) args[0]).isInstance(proxy)) {
            return true;
          }
          break;
      }

      if (!hasTargetConnection()) {
        // No physical target Connection kept yet ->
        // resolve transaction demarcation methods without fetching
        // a physical JDBC Connection until absolutely necessary.

        switch (method.getName()) {
          case "toString":
            return "Lazy Connection proxy for target DataSource [" + getTargetDataSource() + "]";
          case "getAutoCommit":
            if (this.autoCommit != null) {
              return this.autoCommit;
            }
            // Else fetch actual Connection and check there,
            // because we didn't have a default specified.
            break;
          case "setAutoCommit":
            this.autoCommit = (Boolean) args[0];
            return null;
          case "getTransactionIsolation":
            if (this.transactionIsolation != null) {
              return this.transactionIsolation;
            }
            // Else fetch actual Connection and check there,
            // because we didn't have a default specified.
            break;
          case "setTransactionIsolation":
            this.transactionIsolation = (Integer) args[0];
            return null;
          case "isReadOnly":
            return this.readOnly;
          case "setReadOnly":
            this.readOnly = (Boolean) args[0];
            return null;
          case "getHoldability":
            return this.holdability;
          case "setHoldability":
            this.holdability = (Integer) args[0];
            return null;
          case "commit":
          case "rollback":
            // Ignore: no statements created yet.
            return null;
          case "getWarnings":
          case "clearWarnings":
            // Ignore: no warnings to expose yet.
            return null;
          case "close":
            // Ignore: no target connection yet.
            this.closed = true;
            return null;
          case "isClosed":
            return this.closed;
          default:
            if (this.closed) {
              // Connection proxy closed, without ever having fetched a
              // physical JDBC Connection: throw corresponding SQLException.
              throw new SQLException("Illegal operation: connection is closed");
            }
        }
      }

      // Target Connection already fetched,
      // or target Connection necessary for current operation ->
      // invoke method on target connection.
      try {
        return method.invoke(getTargetConnection(method), args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }

    /**
     * Return whether the proxy currently holds a target Connection.
     */
    private boolean hasTargetConnection() {
      return (this.target != null);
    }

    /**
     * Return the target Connection, fetching it and initializing it if necessary.
     */
    private Connection getTargetConnection(Method operation) throws SQLException {
      if (this.target == null) {
        // No target Connection held -> fetch one.
        if (logger.isTraceEnabled()) {
          logger.trace("Connecting to database for operation '" + operation.getName() + "'");
        }

        // Fetch physical Connection from DataSource.
        this.target = (this.username != null) ?
                      obtainTargetDataSource().getConnection(this.username, this.password) :
                      obtainTargetDataSource().getConnection();

        // If we still lack default connection properties, check them now.
        checkDefaultConnectionProperties(this.target);

        // Apply kept transaction settings, if any.
        if (this.readOnly) {
          try {
            this.target.setReadOnly(true);
          }
          catch (Exception ex) {
            // "read-only not supported" -> ignore, it's just a hint anyway
            logger.debug("Could not set JDBC Connection read-only", ex);
          }
        }
        if (this.transactionIsolation != null &&
                !this.transactionIsolation.equals(defaultTransactionIsolation())) {
          this.target.setTransactionIsolation(this.transactionIsolation);
        }
        if (this.autoCommit != null && this.autoCommit != this.target.getAutoCommit()) {
          this.target.setAutoCommit(this.autoCommit);
        }
      }

      else {
        // Target Connection already held -> return it.
        if (logger.isTraceEnabled()) {
          logger.trace("Using existing database connection for operation '" + operation.getName() + "'");
        }
      }

      return this.target;
    }
  }

}
