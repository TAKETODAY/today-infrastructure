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

package infra.restclient.test.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestTemplateBuilder;

import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RestClientTest @RestClientTest} with no specific client.
 *
 * @author Phillip Webb
 */
@RestClientTest
class RestClientTestNoComponentIntegrationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RestTemplateBuilder restTemplateBuilder;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void exampleRestClientIsNotInjected() {
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.applicationContext.getBean(ExampleRestTemplateService.class));
  }

  @Test
  void examplePropertiesIsNotInjected() {
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.applicationContext.getBean(ExampleProperties.class));
  }

  @Test
  void manuallyCreateBean() {
    ExampleRestTemplateService client = new ExampleRestTemplateService(this.restTemplateBuilder);
    this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
    assertThat(client.test()).isEqualTo("hello");
  }

}
