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

import infra.app.test.RandomPortWebServerConfig;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestRestTemplateContextCustomizer} to ensure
 * early-initialization of factory beans doesn't occur.
 *
 * @author Madhura Bhave
 */
@InfraTest(classes = TestRestTemplateContextCustomizerWithFactoryBeanTests.TestClassWithFactoryBean.class,
           webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TestRestTemplateContextCustomizerWithFactoryBeanTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void test() {
    assertThat(this.restTemplate).isNotNull();
  }

  @Import(RandomPortWebServerConfig.class)
  @Configuration(proxyBeanMethods = false)
  @ComponentScan("infra.app.test.web.client.scan")
  static class TestClassWithFactoryBean {

  }

}
