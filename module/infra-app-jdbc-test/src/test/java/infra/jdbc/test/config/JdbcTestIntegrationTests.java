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

import java.util.Collection;

import javax.sql.DataSource;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.simple.JdbcClient;
import infra.test.context.TestPropertySource;
import infra.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import infra.transaction.config.TransactionAutoConfiguration;
import infra.transaction.config.TransactionManagerCustomizationAutoConfiguration;

import static infra.app.config.AutoConfigurationImportedCondition.importedAutoConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link JdbcTest @JdbcTest}.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
@JdbcTest
@TestPropertySource(
        properties = "sql.init.schemaLocations=classpath:infra/jdbc/test/config/schema.sql")
class JdbcTestIntegrationTests {

  @Autowired
  private JdbcClient jdbcClient;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testJdbcClient() {
    ExampleJdbcClientRepository repository = new ExampleJdbcClientRepository(this.jdbcClient);
    repository.save(new ExampleEntity(1, "John"));
    ExampleEntity entity = repository.findById(1);
    assertThat(entity.getId()).isOne();
    assertThat(entity.getName()).isEqualTo("John");
    Collection<ExampleEntity> entities = repository.findAll();
    assertThat(entities).hasSize(1);
    entity = entities.iterator().next();
    assertThat(entity.getId()).isOne();
    assertThat(entity.getName()).isEqualTo("John");
  }

  @Test
  void testJdbcTemplate() {
    ExampleRepository repository = new ExampleRepository(this.jdbcTemplate);
    repository.save(new ExampleEntity(1, "John"));
    ExampleEntity entity = repository.findById(1);
    assertThat(entity.getId()).isOne();
    assertThat(entity.getName()).isEqualTo("John");
    Collection<ExampleEntity> entities = repository.findAll();
    assertThat(entities).hasSize(1);
    entity = entities.iterator().next();
    assertThat(entity.getId()).isOne();
    assertThat(entity.getName()).isEqualTo("John");
  }

  @Test
  void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
    String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
    assertThat(product).isEqualTo("H2");
  }

  @Test
  void didNotInjectExampleRepository() {
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.applicationContext.getBean(ExampleRepository.class));
  }

  @Test
  void serviceConnectionAutoConfigurationWasImported() {
    assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
  }

  @Test
  void transactionAutoConfigurationWasImported() {
    assertThat(this.applicationContext).has(importedAutoConfiguration(TransactionAutoConfiguration.class));
    assertThat(this.applicationContext)
            .has(importedAutoConfiguration(TransactionManagerCustomizationAutoConfiguration.class));
  }

}
