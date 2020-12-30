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
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.jdbc.utils.DataSourceUtils;
import cn.taketoday.transaction.ConnectionHolder;
import cn.taketoday.transaction.SynchronizationManager;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2018-10-09 11:58
 */
@Setter
public class DefaultTransaction implements Transaction {

  private static final Logger log = LoggerFactory.getLogger(DefaultTransaction.class);
  public static boolean debugEnabled = log.isDebugEnabled();

  private boolean autoCommit;
  private Connection connection;
  private final DataSource dataSource;

  public DefaultTransaction(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  private void openConnection() throws SQLException {
    this.connection = DataSourceUtils.getConnection(this.dataSource);
    this.autoCommit = this.connection.getAutoCommit();
    if (debugEnabled) {
      log.debug("JDBC Connection [{}] will be managed by Today Context", this.connection);
    }
  }

  @Override
  public void commit() throws SQLException {
    if (this.connection != null && !this.autoCommit) {
      if (debugEnabled) {
        log.debug("Committing JDBC Connection [{}]", connection);
      }
      this.connection.commit();
    }
  }

  @Override
  public void rollback() throws SQLException {
    if (this.connection != null && !this.autoCommit) {
      if (debugEnabled) {
        log.debug("Rolling back JDBC Connection [{}]", connection);
      }
      this.connection.rollback();
    }
  }

  @Override
  public void close() throws SQLException {
    DataSourceUtils.releaseConnection(this.connection, this.dataSource);
  }

  @Override
  public Integer getTimeout() throws SQLException {

    ConnectionHolder holder = (ConnectionHolder) SynchronizationManager.getResource(dataSource);
    if (holder != null && holder.hasTimeout()) {
      return holder.getTimeToLiveInSeconds();
    }
    return null;
  }

}
