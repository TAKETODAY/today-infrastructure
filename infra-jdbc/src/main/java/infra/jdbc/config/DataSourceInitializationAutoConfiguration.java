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

import javax.sql.DataSource;

import infra.context.annotation.Import;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.context.properties.EnableConfigurationProperties;
import infra.jdbc.datasource.SimpleDriverDataSource;
import infra.sql.config.init.ApplicationScriptDatabaseInitializer;
import infra.sql.config.init.ConditionalOnSqlInitialization;
import infra.sql.config.init.SqlInitializationProperties;
import infra.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import infra.stereotype.Component;
import infra.util.StringUtils;

/**
 * Auto-configuration for {@link DataSource} initialization.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(ApplicationScriptDatabaseInitializer.class)
@ConditionalOnMissingBean(ApplicationScriptDatabaseInitializer.class)
@ConditionalOnSingleCandidate(DataSource.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@EnableConfigurationProperties(SqlInitializationProperties.class)
@ConditionalOnSqlInitialization
public final class DataSourceInitializationAutoConfiguration {

  @Component
  static ApplicationDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
          SqlInitializationProperties properties) {
    return new ApplicationDataSourceScriptDatabaseInitializer(
            determineDataSource(dataSource, properties.getUsername(), properties.getPassword()), properties);
  }

  private static DataSource determineDataSource(DataSource dataSource, @Nullable String username,
          @Nullable String password) {
    if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
      return DataSourceBuilder.derivedFrom(dataSource)
              .username(username)
              .password(password)
              .type(SimpleDriverDataSource.class)
              .build();
    }
    return dataSource;
  }

}


