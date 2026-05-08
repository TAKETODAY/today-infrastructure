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

package infra.jdbc.test.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.jdbc.config.EmbeddedDatabaseConnection;
import infra.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcTest @JdbcTest}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@JdbcTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.HSQLDB)
@TestPropertySource(properties = "infra.test.database.replace=AUTO_CONFIGURED")
class JdbcTestWithAutoConfigureTestDatabaseReplacePropertyAutoConfiguredIntegrationTests {

  @Autowired
  private DataSource dataSource;

  @Test
  void replacesAutoConfiguredDataSource() throws Exception {
    String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
    assertThat(product).startsWith("HSQL");
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration // Will auto-configure H2
  static class Config {

  }

}
