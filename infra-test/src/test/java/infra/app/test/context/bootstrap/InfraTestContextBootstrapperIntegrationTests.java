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

package infra.app.test.context.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.context.InfraTestContextBootstrapper;
import infra.app.test.context.TestConfiguration;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.test.context.BootstrapWith;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfraTestContextBootstrapper} (in its own package so
 * we can test detection).
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@BootstrapWith(InfraTestContextBootstrapper.class)
class InfraTestContextBootstrapperIntegrationTests {

  @Autowired
  private ApplicationContext context;

  @Autowired
  private InfraTestContextBootstrapperExampleConfig config;

  @Test
  void findConfigAutomatically() {
    assertThat(this.config).isNotNull();
  }

  @Test
  void contextWasCreatedViaInfraApplication() {
    assertThat(this.context.getId()).startsWith("application");
  }

  @Test
  void testConfigurationWasApplied() {
    assertThat(this.context.getBean(ExampleBean.class)).isNotNull();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    ExampleBean exampleBean() {
      return new ExampleBean();
    }

  }

  static class ExampleBean {

  }

}
