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

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * {@link DataSourcePoolMetadata} for an Apache Commons DBCP2 {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CommonsDbcp2DataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<BasicDataSource> {

  public CommonsDbcp2DataSourcePoolMetadata(BasicDataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Integer getActive() {
    return getDataSource().getNumActive();
  }

  @Override
  public Integer getIdle() {
    return getDataSource().getNumIdle();
  }

  @Override
  public Integer getMax() {
    return getDataSource().getMaxTotal();
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
    return getDataSource().getDefaultAutoCommit();
  }

}
