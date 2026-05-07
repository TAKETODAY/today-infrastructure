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

package infra.testcontainers.properties;

import org.junit.jupiter.api.Test;

import infra.app.InfraConfiguration;
import infra.app.test.context.InfraTest;
import infra.app.test.context.TestConfiguration;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.env.Environment;
import infra.test.context.DynamicPropertyRegistrar;
import infra.testcontainers.properties.TestcontainersPropertySourceAutoConfigurationWithInfraTestIntegrationTests.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestcontainersPropertySourceAutoConfiguration} when combined with
 * {@link InfraTest @InfraTest}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@InfraTest(classes = TestConfig.class,
        properties = "infra.testcontainers.dynamic-property-registry-injection=allow")
class TestcontainersPropertySourceAutoConfigurationWithInfraTestIntegrationTests {

  @Autowired
  private Environment environment;

  @Test
  void callsRegistrars() {
    assertThat(this.environment.getProperty("from.registrar")).isEqualTo("two");
  }

  @TestConfiguration
  @ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration.class)
  @InfraConfiguration
  static class TestConfig {

    @Bean
    DynamicPropertyRegistrar propertyRegistrar() {
      return (registry) -> registry.add("from.registrar", () -> "two");
    }

  }

}
