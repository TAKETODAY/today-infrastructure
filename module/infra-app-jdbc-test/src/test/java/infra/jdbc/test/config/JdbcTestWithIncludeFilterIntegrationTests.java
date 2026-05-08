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

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.ComponentScan.Filter;
import infra.stereotype.Repository;
import infra.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for {@link JdbcTest @JdbcTest}.
 *
 * @author Stephane Nicoll
 */
@JdbcTest(includeFilters = @Filter(Repository.class))
@TestPropertySource(
        properties = "sql.init.schemaLocations=classpath:infra/jdbc/test/config/schema.sql")
class JdbcTestWithIncludeFilterIntegrationTests {

  @Autowired
  private ExampleRepository repository;

  @Test
  void testRepository() {
    this.repository.save(new ExampleEntity(42, "Smith"));
    ExampleEntity entity = this.repository.findById(42);
    assertThat(entity).isNotNull();
    assertThat(entity.getName()).isEqualTo("Smith");
  }

}
