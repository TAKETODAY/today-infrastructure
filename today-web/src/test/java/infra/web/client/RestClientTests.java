/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 16:39
 */
class RestClientTests {

  @Test
  void createMethodReturnsRestClient() {
    RestClient client = RestClient.create();

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(DefaultRestClient.class);
  }

  @Test
  void createWithBaseURIReturnsRestClient() {
    String baseURI = "http://example.com/api";
    RestClient client = RestClient.create(baseURI);

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(DefaultRestClient.class);
  }

  @Test
  void createWithBaseURIReturnsRestClientWithURI() {
    URI baseURI = URI.create("http://example.com/api");
    RestClient client = RestClient.create(baseURI);

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(DefaultRestClient.class);
  }

  @Test
  void builderMethodReturnsBuilder() {
    RestClient.Builder builder = RestClient.builder();

    assertThat(builder).isNotNull();
    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
  }

  @Test
  void getMethodReturnsRequestHeadersUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestHeadersUriSpec<?> spec = client.get();

    assertThat(spec).isNotNull();
  }

  @Test
  void getWithURIMethodReturnsRequestHeadersSpec() {
    RestClient client = RestClient.create();
    URI uri = URI.create("http://example.com/test");

    RestClient.RequestHeadersSpec<?> spec = client.get(uri);

    assertThat(spec).isNotNull();
  }

  @Test
  void getWithStringUriVariablesReturnsRequestHeadersSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestHeadersSpec<?> spec = client.get("/test/{id}", 123);

    assertThat(spec).isNotNull();
  }

  @Test
  void headMethodReturnsRequestHeadersUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestHeadersUriSpec<?> spec = client.head();

    assertThat(spec).isNotNull();
  }

  @Test
  void postMethodReturnsRequestBodyUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestBodyUriSpec spec = client.post();

    assertThat(spec).isNotNull();
  }

  @Test
  void putMethodReturnsRequestBodyUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestBodyUriSpec spec = client.put();

    assertThat(spec).isNotNull();
  }

  @Test
  void patchMethodReturnsRequestBodyUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestBodyUriSpec spec = client.patch();

    assertThat(spec).isNotNull();
  }

  @Test
  void deleteMethodReturnsRequestBodyUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestBodyUriSpec spec = client.delete();

    assertThat(spec).isNotNull();
  }

  @Test
  void optionsMethodReturnsRequestHeadersUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestHeadersUriSpec<?> spec = client.options();

    assertThat(spec).isNotNull();
  }

  @Test
  void methodWithHttpMethodReturnsRequestBodyUriSpec() {
    RestClient client = RestClient.create();

    RestClient.RequestBodyUriSpec spec = client.method(HttpMethod.GET);

    assertThat(spec).isNotNull();
  }

  @Test
  void mutateReturnsBuilder() {
    RestClient client = RestClient.create();

    RestClient.Builder builder = client.mutate();

    assertThat(builder).isNotNull();
    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
  }

}