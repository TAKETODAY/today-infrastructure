/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.datasource;

import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.util.logging.Logger;

import javax.sql.DataSource;

import infra.beans.factory.InitializingBean;
import infra.lang.Assert;

/**
 * JDBC {@link DataSource} implementation that delegates all calls
 * to a given target {@link DataSource}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target DataSource.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #getConnection
 * @since 4.0
 */
public class DelegatingDataSource implements DataSource, InitializingBean {

  private @Nullable DataSource targetDataSource;

  /**
   * Create a new DelegatingDataSource.
   *
   * @see #setTargetDataSource
   */
  public DelegatingDataSource() {
  }

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
  public @Nullable DataSource getTargetDataSource() {
    return this.targetDataSource;
  }

  /**
   * Obtain the target {@code DataSource} for actual use (never {@code null}).
   */
  protected DataSource targetDataSource() {
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
    return targetDataSource().getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return targetDataSource().getConnection(username, password);
  }

  @Override
  public ConnectionBuilder createConnectionBuilder() throws SQLException {
    return targetDataSource().createConnectionBuilder();
  }

  @Override
  public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
    return targetDataSource().createShardingKeyBuilder();
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return targetDataSource().getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    targetDataSource().setLogWriter(out);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return targetDataSource().getLoginTimeout();
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    targetDataSource().setLoginTimeout(seconds);
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
    return targetDataSource().unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return (iface.isInstance(this) || targetDataSource().isWrapperFor(iface));
  }

  //---------------------------------------------------------------------
  // Implementation of JDBC 4.1's getParentLogger method
  //---------------------------------------------------------------------

  @Override
  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
