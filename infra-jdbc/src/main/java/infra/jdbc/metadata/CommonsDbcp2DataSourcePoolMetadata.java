/*
 * Copyright 2012-present the original author or authors.
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

package infra.jdbc.metadata;

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
