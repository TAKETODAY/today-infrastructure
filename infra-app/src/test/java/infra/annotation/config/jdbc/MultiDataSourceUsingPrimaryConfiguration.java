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

package infra.annotation.config.jdbc;

import javax.sql.DataSource;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;

/**
 * Configuration for multiple {@link DataSource} (one being {@code @Primary}).
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
class MultiDataSourceUsingPrimaryConfiguration {

  @Bean
  @Primary
  DataSource test1DataSource() {
    return new TestDataSource("test1", false);
  }

  @Bean
  DataSource test2DataSource() {
    return new TestDataSource("test2", false);
  }

}
