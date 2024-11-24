/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.test.context.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.app.test.context.InfraTestContextBootstrapper;
import infra.app.test.context.TestConfiguration;
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
  void contextWasCreatedViaSpringApplication() {
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
