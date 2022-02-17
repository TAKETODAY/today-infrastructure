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

package cn.taketoday.orm.mybatis.transaction;

import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.jdbc.datasource.ConnectionHolder;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * {@code TodayManagedTransaction} handles the lifecycle of a JDBC connection. It retrieves a connection from Spring's
 * transaction manager and returns it back to it when it is no longer needed.
 * <p>
 * If Spring's transaction handling is active it will no-op all commit/rollback/close calls assuming that the Spring
 * transaction manager will do the job.
 * <p>
 * If it is not it will behave like {@code JdbcTransaction}.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
public class ManagedTransaction implements Transaction {
  private static final Logger log = LoggerFactory.getLogger(ManagedTransaction.class);

  private final DataSource dataSource;

  private Connection connection;

  private boolean isConnectionTransactional;

  private boolean autoCommit;

  public ManagedTransaction(DataSource dataSource) {
    Assert.notNull(dataSource, "No DataSource specified");
    this.dataSource = dataSource;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Connection getConnection() throws SQLException {
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  /**
   * Gets a connection from Framework transaction manager and discovers if this {@code Transaction} should manage
   * connection or let it to Framework.
   * <p>
   * It also reads autocommit setting because when using Framework Transaction MyBatis thinks that autocommit is always
   * false and will always call commit/rollback so we need to no-op that calls.
   */
  private void openConnection() throws SQLException {
    this.connection = DataSourceUtils.getConnection(this.dataSource);
    this.autoCommit = this.connection.getAutoCommit();
    this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);
    if (log.isDebugEnabled()) {
      log.debug("JDBC Connection [{}] will{}be managed by TODAY", connection, (this.isConnectionTransactional ? " " : " not "));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws SQLException {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [{}]", connection);
      }
      this.connection.commit();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws SQLException {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      if (log.isDebugEnabled()) {
        log.debug("Rolling back JDBC Connection [{}]", connection);
      }
      this.connection.rollback();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws SQLException {
    DataSourceUtils.releaseConnection(this.connection, this.dataSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getTimeout() throws SQLException {
    ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    if (holder != null && holder.hasTimeout()) {
      return holder.getTimeToLiveInSeconds();
    }
    return null;
  }

}
