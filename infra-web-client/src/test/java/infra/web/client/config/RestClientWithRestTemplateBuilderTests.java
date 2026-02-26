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

package infra.web.client.config;

import org.junit.jupiter.api.Test;

import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestClient;
import infra.web.client.RestClient.Builder;
import infra.web.client.RestTemplate;
import infra.web.client.RestTemplateBuilder;

import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for building a {@link RestClient} from a {@link RestTemplateBuilder}.
 *
 * @author Scott Frederick
 */
class RestClientWithRestTemplateBuilderTests {

  @Test
  void buildUsingRestTemplateBuilderRootUri() {
    RestTemplate restTemplate = new RestTemplateBuilder().baseURI("https://resttemplate.example.com").build();
    Builder builder = RestClient.builder(restTemplate);
    RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  @Test
  void buildUsingRestClientBuilderBaseURI() {
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    Builder builder = RestClient.builder(restTemplate).baseURI("https://restclient.example.com");
    RestClient client = buildMockedClient(builder, "https://restclient.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  @Test
  void buildRestTemplateBuilderRootUriAndRestClientBuilderBaseURI() {
    RestTemplate restTemplate = new RestTemplateBuilder().baseURI("https://resttemplate.example.com").build();
    Builder builder = RestClient.builder(restTemplate).baseURI("https://restclient.example.com");
    RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  private RestClient buildMockedClient(Builder builder, String url) {
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(url)).andRespond(withSuccess());
    return builder.build();
  }

}
