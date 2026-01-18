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

package infra.app.rest.client;

import org.junit.jupiter.api.Test;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.annotation.DirtiesContext;
import infra.web.server.netty.RandomPortWebServerConfig;

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
