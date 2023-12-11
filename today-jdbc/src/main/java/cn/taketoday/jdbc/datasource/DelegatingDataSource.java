/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.util.logging.Logger;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * JDBC {@link DataSource} implementation that delegates all calls
 * to a given target {@link DataSource}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target DataSource.
 *
 * @author Juergen Hoeller
 * @see #getConnection
 * @since 4.0
 */
public class DelegatingDataSource implements DataSource, InitializingBean {

  @Nullable
  private DataSource targetDataSource;

  /**
   * Create a new DelegatingDataSource.
   *
   * @see #setTargetDataSource
   */
  public DelegatingDataSource() { }

  /**
   * Create a new DelegatingDataSource.
   *
   * @param targetDataSource the target DataSource
   */
  public DelegatingDataSource(DataSource targetDataSource) {
    setTargetDataSource(targetDataSource);
  }

  /**
   * Set the target DataSource that this DataSource should delegate to.
   */
  public void setTargetDataSource(@Nullable DataSource targetDataSource) {
    this.targetDataSource = targetDataSource;
  }

  /**
   * Return the target DataSource that this DataSource should delegate to.
   */
  @Nullable
  public DataSource getTargetDataSource() {
    return this.targetDataSource;
  }

  /**
   * Obtain the target {@code DataSource} for actual use (never {@code null}).
   */
  protected DataSource obtainTargetDataSource() {
    DataSource dataSource = getTargetDataSource();
    Assert.state(dataSource != null, "No 'targetDataSource' set");
    return dataSource;
  }

  @Override
  public void afterPropertiesSet() {
    if (getTargetDataSource() == null) {
      throw new IllegalArgumentException("Property 'targetDataSource' is required");
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return obtainTargetDataSource().getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return obtainTargetDataSource().getConnection(username, password);
  }

  @Override
  public ConnectionBuilder createConnectionBuilder() throws SQLException {
    return obtainTargetDataSource().createConnectionBuilder();
  }

  @Override
  public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
    return obtainTargetDataSource().createShardingKeyBuilder();
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return obtainTargetDataSource().getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    obtainTargetDataSource().setLogWriter(out);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return obtainTargetDataSource().getLoginTimeout();
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    obtainTargetDataSource().setLoginTimeout(seconds);
  }

  //---------------------------------------------------------------------
  // Implementation of JDBC 4.0's Wrapper interface
  //---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return (T) this;
    }
    return obtainTargetDataSource().unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return (iface.isInstance(this) || obtainTargetDataSource().isWrapperFor(iface));
  }

  //---------------------------------------------------------------------
  // Implementation of JDBC 4.1's getParentLogger method
  //---------------------------------------------------------------------

  @Override
  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
