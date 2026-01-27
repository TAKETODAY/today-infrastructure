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

package infra.jdbc.config;

import org.jspecify.annotations.Nullable;

/**
 * Adapts {@link DataSourceProperties} to {@link JdbcConnectionDetails}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
final class PropertiesJdbcConnectionDetails implements JdbcConnectionDetails {

  private final DataSourceProperties properties;

  PropertiesJdbcConnectionDetails(DataSourceProperties properties) {
    this.properties = properties;
  }

  @Override
  public @Nullable String getUsername() {
    return this.properties.determineUsername();
  }

  @Override
  public @Nullable String getPassword() {
    return this.properties.determinePassword();
  }

  @Override
  public String getJdbcUrl() {
    return this.properties.determineUrl();
  }

  @Override
  public String getDriverClassName() {
    return this.properties.determineDriverClassName();
  }

  @Override
  public @Nullable String getXaDataSourceClassName() {
    return properties.getXa().getDataSourceClassName() != null
            ? properties.getXa().getDataSourceClassName()
            : JdbcConnectionDetails.super.getXaDataSourceClassName();
  }

}
