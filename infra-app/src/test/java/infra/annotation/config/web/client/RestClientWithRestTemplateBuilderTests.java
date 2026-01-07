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

package infra.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestClient;
import infra.web.client.RestClient.Builder;
import infra.web.client.RestTemplate;
import infra.web.client.config.RestTemplateBuilder;

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
    RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://resttemplate.example.com").build();
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
    RestTemplate restTemplate = new RestTemplateBuilder().rootUri("https://resttemplate.example.com").build();
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
