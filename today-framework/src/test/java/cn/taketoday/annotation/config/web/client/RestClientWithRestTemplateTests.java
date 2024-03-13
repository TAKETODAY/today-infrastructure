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

package cn.taketoday.annotation.config.web.client;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.RestClient.Builder;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.util.DefaultUriBuilderFactory;

import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for building a {@link RestClient} from a {@link RestTemplate}.
 *
 * @author Scott Frederick
 */
class RestClientWithRestTemplateTests {

  @Test
  void buildUsingRestTemplateUriTemplateHandler() {
    RestTemplate restTemplate = new RestTemplate();
    DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("https://resttemplate.example.com");
    restTemplate.setUriTemplateHandler(uriBuilderFactory);
    Builder builder = RestClient.builder(restTemplate);
    RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  @Test
  void buildUsingRestClientBuilderBaseUrl() {
    RestTemplate restTemplate = new RestTemplate();
    Builder builder = RestClient.builder(restTemplate).baseUrl("https://restclient.example.com");
    RestClient client = buildMockedClient(builder, "https://restclient.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  @Test
  void buildUsingRestTemplateUriTemplateHandlerAndRestClientBuilderBaseUrl() {
    RestTemplate restTemplate = new RestTemplate();
    DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("https://resttemplate.example.com");
    restTemplate.setUriTemplateHandler(uriBuilderFactory);
    Builder builder = RestClient.builder(restTemplate).baseUrl("https://restclient.example.com");
    RestClient client = buildMockedClient(builder, "https://resttemplate.example.com/test");
    assertThat(client.get().uri("/test").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful()).isTrue();
  }

  private RestClient buildMockedClient(Builder builder, String url) {
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(url)).andRespond(withSuccess());
    return builder.build();
  }

}
