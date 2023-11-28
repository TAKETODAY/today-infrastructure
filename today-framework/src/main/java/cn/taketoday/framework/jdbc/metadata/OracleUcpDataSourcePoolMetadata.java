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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.jdbc.metadata;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.util.StringUtils;
import oracle.ucp.jdbc.PoolDataSource;

/**
 * {@link DataSourcePoolMetadata} for an Oracle UCP {@link DataSource}.
 *
 * @author Fabio Grassi
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OracleUcpDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<PoolDataSource> {

  public OracleUcpDataSourcePoolMetadata(PoolDataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Integer getActive() {
    try {
      return getDataSource().getBorrowedConnectionsCount();
    }
    catch (SQLException ex) {
      return null;
    }
  }

  @Override
  public Integer getIdle() {
    try {
      return getDataSource().getAvailableConnectionsCount();
    }
    catch (SQLException ex) {
      return null;
    }
  }

  @Override
  public Integer getMax() {
    return getDataSource().getMaxPoolSize();
  }

  @Override
  public Integer getMin() {
    return getDataSource().getMinPoolSize();
  }

  @Override
  public String getValidationQuery() {
    return getDataSource().getSQLForValidateConnection();
  }

  @Override
  public Boolean getDefaultAutoCommit() {
    String autoCommit = getDataSource().getConnectionProperty("autoCommit");
    return StringUtils.hasText(autoCommit) ? Boolean.valueOf(autoCommit) : null;
  }

}
