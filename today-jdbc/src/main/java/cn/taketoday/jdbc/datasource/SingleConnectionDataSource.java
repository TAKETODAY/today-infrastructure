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

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.jdbc.support.WrappedConnection;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Implementation of {@link SmartDataSource} that wraps a single JDBC Connection
 * which is not closed after use. Obviously, this is not multi-threading capable.
 *
 * <p>Note that at shutdown, someone should close the underlying Connection
 * via the {@code close()} method. Client code will never call close
 * on the Connection handle if it is SmartDataSource-aware (e.g. uses
 * {@code DataSourceUtils.releaseConnection}).
 *
 * <p>If client code will call {@code close()} in the assumption of a pooled
 * Connection, like when using persistence tools, set "suppressClose" to "true".
 * This will return a close-suppressing proxy instead of the physical Connection.
 *
 * <p>This is primarily intended for testing. For example, it enables easy testing
 * outside an application server, for code that expects to work on a DataSource.
 * In contrast to {@link DriverManagerDataSource}, it reuses the same Connection
 * all the time, avoiding excessive creation of physical Connections.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection()
 * @see Connection#close()
 * @see DataSourceUtils#releaseConnection
 * @since 4.0
 */
public class SingleConnectionDataSource extends DriverManagerDataSource implements SmartDataSource, DisposableBean {

  /** Create a close-suppressing proxy?. */
  private boolean suppressClose;

  /** Override auto-commit state?. */
  @Nullable
  private Boolean autoCommit;

  /** Wrapped Connection. */
  @Nullable
  private Connection target;

  /** Proxy Connection. */
  @Nullable
  private Connection connection;

  /** Synchronization monitor for the shared Connection. */
  private final Object connectionMonitor = new Object();

  /**
   * Constructor for bean-style configuration.
   */
  public SingleConnectionDataSource() { }

  /**
   * Create a new SingleConnectionDataSource with the given standard
   * DriverManager parameters.
   *
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param username the JDBC username to use for accessing the DriverManager
   * @param password the JDBC password to use for accessing the DriverManager
   * @param suppressClose if the returned Connection should be a
   * close-suppressing proxy or the physical Connection
   * @see java.sql.DriverManager#getConnection(String, String, String)
   */
  public SingleConnectionDataSource(String url, String username, String password, boolean suppressClose) {
    super(url, username, password);
    this.suppressClose = suppressClose;
  }

  /**
   * Create a new SingleConnectionDataSource with the given standard
   * DriverManager parameters.
   *
   * @param url the JDBC URL to use for accessing the DriverManager
   * @param suppressClose if the returned Connection should be a
   * close-suppressing proxy or the physical Connection
   * @see java.sql.DriverManager#getConnection(String, String, String)
   */
  public SingleConnectionDataSource(String url, boolean suppressClose) {
    super(url);
    this.suppressClose = suppressClose;
  }

  /**
   * Create a new SingleConnectionDataSource with a given Connection.
   *
   * @param target underlying target Connection
   * @param suppressClose if the Connection should be wrapped with a Connection that
   * suppresses {@code close()} calls (to allow for normal {@code close()}
   * usage in applications that expect a pooled Connection but do not know our
   * SmartDataSource interface)
   */
  public SingleConnectionDataSource(Connection target, boolean suppressClose) {
    Assert.notNull(target, "Connection must not be null");
    this.target = target;
    this.suppressClose = suppressClose;
    this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
  }

  /**
   * Set whether the returned Connection should be a close-suppressing proxy
   * or the physical Connection.
   */
  public void setSuppressClose(boolean suppressClose) {
    this.suppressClose = suppressClose;
  }

  /**
   * Return whether the returned Connection will be a close-suppressing proxy
   * or the physical Connection.
   */
  protected boolean isSuppressClose() {
    return this.suppressClose;
  }

  /**
   * Set whether the returned Connection's "autoCommit" setting should be overridden.
   */
  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = (autoCommit);
  }

  /**
   * Return whether the returned Connection's "autoCommit" setting should be overridden.
   *
   * @return the "autoCommit" value, or {@code null} if none to be applied
   */
  @Nullable
  protected Boolean getAutoCommitValue() {
    return this.autoCommit;
  }

  @Override
  public Connection getConnection() throws SQLException {
    synchronized(this.connectionMonitor) {
      if (this.connection == null) {
        // No underlying Connection -> lazy init via DriverManager.
        initConnection();
      }
      if (this.connection.isClosed()) {
        throw new SQLException(
                "Connection was closed in SingleConnectionDataSource. Check that user code checks " +
                        "shouldClose() before closing Connections, or set 'suppressClose' to 'true'");
      }
      return this.connection;
    }
  }

  /**
   * Specifying a custom username and password doesn't make sense
   * with a single Connection. Returns the single Connection if given
   * the same username and password; throws an SQLException else.
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (ObjectUtils.nullSafeEquals(username, getUsername()) &&
            ObjectUtils.nullSafeEquals(password, getPassword())) {
      return getConnection();
    }
    else {
      throw new SQLException("SingleConnectionDataSource does not support custom username and password");
    }
  }

  /**
   * This is a single Connection: Do not close it when returning to the "pool".
   */
  @Override
  public boolean shouldClose(Connection con) {
    synchronized(this.connectionMonitor) {
      return (con != this.connection && con != this.target);
    }
  }

  /**
   * Close the underlying Connection.
   * The provider of this DataSource needs to care for proper shutdown.
   * <p>As this bean implements DisposableBean, a bean factory will
   * automatically invoke this on destruction of its cached singletons.
   */
  @Override
  public void destroy() {
    synchronized(this.connectionMonitor) {
      closeConnection();
    }
  }

  /**
   * Initialize the underlying Connection via the DriverManager.
   */
  public void initConnection() throws SQLException {
    if (getUrl() == null) {
      throw new IllegalStateException("'url' property is required for lazily initializing a Connection");
    }
    synchronized(this.connectionMonitor) {
      closeConnection();
      this.target = getConnectionFromDriver(getUsername(), getPassword());
      prepareConnection(this.target);
      if (logger.isDebugEnabled()) {
        logger.debug("Established shared JDBC Connection: {}", this.target);
      }
      this.connection = (isSuppressClose() ? getCloseSuppressingConnectionProxy(this.target) : this.target);
    }
  }

  /**
   * Reset the underlying shared Connection, to be reinitialized on next access.
   */
  public void resetConnection() {
    synchronized(this.connectionMonitor) {
      closeConnection();
      this.target = null;
      this.connection = null;
    }
  }

  /**
   * Prepare the given Connection before it is exposed.
   * <p>The default implementation applies the auto-commit flag, if necessary.
   * Can be overridden in subclasses.
   *
   * @param con the Connection to prepare
   * @see #setAutoCommit
   */
  protected void prepareConnection(Connection con) throws SQLException {
    Boolean autoCommit = getAutoCommitValue();
    if (autoCommit != null && con.getAutoCommit() != autoCommit) {
      con.setAutoCommit(autoCommit);
    }
  }

  /**
   * Close the underlying shared Connection.
   */
  private void closeConnection() {
    if (this.target != null) {
      try {
        this.target.close();
      }
      catch (Throwable ex) {
        logger.info("Could not close shared JDBC Connection", ex);
      }
    }
  }

  /**
   * Wrap the given Connection with a proxy that delegates every method call to it
   * but suppresses close calls.
   *
   * @param target the original Connection to wrap
   * @return the wrapped Connection
   */
  protected Connection getCloseSuppressingConnectionProxy(Connection target) {
    return new CloseSuppressingConnectionProxy(target);
  }

  /**
   * Proxy that suppresses close calls on JDBC Connections.
   */
  static class CloseSuppressingConnectionProxy extends WrappedConnection implements ConnectionProxy {

    public CloseSuppressingConnectionProxy(Connection delegate) {
      super(delegate);
    }

    @Override
    public Connection getTargetConnection() {
      return delegate;
    }

    @Override
    public void close() throws SQLException {
      // noop
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return iface.isInstance(this) ? (T) this : delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }

  }

}
