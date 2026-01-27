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

import javax.sql.DataSource;

import infra.jdbc.init.DataSourceScriptDatabaseInitializer;
import infra.sql.config.init.ApplicationScriptDatabaseInitializer;
import infra.sql.config.init.SqlInitializationProperties;
import infra.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the primary SQL database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ApplicationDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer
        implements ApplicationScriptDatabaseInitializer {

  /**
   * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
   *
   * @param dataSource the primary SQL data source
   * @param properties the SQL initialization properties
   */
  public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
          SqlInitializationProperties properties) {
    this(dataSource, ApplicationScriptDatabaseInitializer.getSettings(properties));
  }

  /**
   * Create a new {@link ApplicationDataSourceScriptDatabaseInitializer} instance.
   *
   * @param dataSource the primary SQL data source
   * @param settings the database initialization settings
   */
  public ApplicationDataSourceScriptDatabaseInitializer(DataSource dataSource,
          DatabaseInitializationSettings settings) {
    super(dataSource, settings);
  }

}
