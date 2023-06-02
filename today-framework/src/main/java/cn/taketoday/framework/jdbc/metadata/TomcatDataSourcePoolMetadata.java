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

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;

/**
 * {@link DataSourcePoolMetadata} for a Tomcat DataSource.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class TomcatDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<DataSource> {

  public TomcatDataSourcePoolMetadata(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Integer getActive() {
    ConnectionPool pool = getDataSource().getPool();
    return (pool != null) ? pool.getActive() : 0;
  }

  @Override
  public Integer getIdle() {
    return getDataSource().getNumIdle();
  }

  @Override
  public Integer getMax() {
    return getDataSource().getMaxActive();
  }

  @Override
  public Integer getMin() {
    return getDataSource().getMinIdle();
  }

  @Override
  public String getValidationQuery() {
    return getDataSource().getValidationQuery();
  }

  @Override
  public Boolean getDefaultAutoCommit() {
    return getDataSource().isDefaultAutoCommit();
  }

}
