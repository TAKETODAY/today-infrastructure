/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.test.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@JUnitConfig(TestDatabaseDynamicPropertySourceTests.Config.class)
@TestPropertySource(properties = "datasource.type=infra.jdbc.datasource.DriverManagerDataSource")
class TestDatabaseDynamicPropertySourceTests {

  private static final String DATABASE_URL = "jdbc:h2:mem:dynamic-test";

  @DynamicPropertySource
  static void jdbcProperties(DynamicPropertyRegistry registry) {
    registry.add("datasource.url", () -> DATABASE_URL);
  }

  @Test
  void autoConfiguredDataSourceWithDynamicUrlIsNotReplaced(@Autowired DriverManagerDataSource dataSource) {
    assertThat(dataSource.getUrl()).isEqualTo(DATABASE_URL);
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ TestDatabaseAutoConfiguration.class, DataSourceAutoConfiguration.class })
  static class Config {

  }

}
