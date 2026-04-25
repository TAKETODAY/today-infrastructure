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

package infra.app.restclient.test.config;

import org.junit.jupiter.api.Test;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestTemplate;
import infra.web.client.RestTemplateBuilder;

import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with a
 * {@link RestTemplate} configured with a root URI.
 *
 * @author Andy Wilkinson
 */
@InfraTest
@AutoConfigureMockRestServiceServer
class AutoConfigureMockRestServiceServerWithRestTemplateRootUriIntegrationTests {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void whenRestTemplateAppliesARootUriThenMockServerExpectationsAreStillMatched() {
    this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
    ResponseEntity<String> entity = this.restTemplate.getForEntity("/test", String.class);
    assertThat(entity.getBody()).isEqualTo("hello");
  }

  @EnableAutoConfiguration
  @Configuration(proxyBeanMethods = false)
  static class RootUriConfiguration {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
      return restTemplateBuilder.baseURI("/rest").build();
    }

  }

}
