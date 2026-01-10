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
