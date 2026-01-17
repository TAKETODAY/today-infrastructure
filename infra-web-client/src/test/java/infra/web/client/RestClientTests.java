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