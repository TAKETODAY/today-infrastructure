/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jdbc.datasource.lookup;

import javax.sql.DataSource;

import infra.lang.Assert;

/**
 * An implementation of the DataSourceLookup that simply wraps a
 * single given DataSource, returned for any data source name.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SingleDataSourceLookup implements DataSourceLookup {

  private final DataSource dataSource;

  /**
   * Create a new instance of the {@link SingleDataSourceLookup} class.
   *
   * @param dataSource the single {@link DataSource} to wrap
   */
  public SingleDataSourceLookup(DataSource dataSource) {
    Assert.notNull(dataSource, "DataSource is required");
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource(String dataSourceName) {
    return this.dataSource;
  }

}
