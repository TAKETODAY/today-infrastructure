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

package infra.app.test.web.client;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.app.test.RandomPortWebServerConfig;
import infra.app.test.context.InfraTest;
import infra.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestRestTemplateContextCustomizer} with a custom
 * {@link TestRestTemplate} bean.
 *
 * @author Phillip Webb
 */
@InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TestRestTemplateContextCustomizerWithOverrideIntegrationTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void test() {
    assertThat(this.restTemplate).isInstanceOf(CustomTestRestTemplate.class);
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ NoTestRestTemplateBeanChecker.class, RandomPortWebServerConfig.class })
  static class TestConfig {

    @Bean
    TestRestTemplate template() {
      return new CustomTestRestTemplate();
    }

  }

  static class CustomTestRestTemplate extends TestRestTemplate {

  }

}
