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

package infra.http.service.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.concurrent.Executor;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.ResponseEntity;
import infra.http.client.ClientHttpResponse;
import infra.http.service.invoker.HttpRequestValues;
import infra.restclient.ClientResponse;
import infra.restclient.RestClient;
import infra.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RestClientAdapterUnitTests {

  private static final URI URI_VALUE = URI.create("https://example.test/items");

  private static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() { };

  private final RestClient client = mock(RestClient.class);

  private final RestClient.RequestBodyUriSpec request = mock(RestClient.RequestBodyUriSpec.class);

  private final Executor executor = Runnable::run;

  private final RestClientAdapter adapter = RestClientAdapter.create(this.client, this.executor);

  private final HttpRequestValues values = HttpRequestValues.builder()
          .setHttpMethod(HttpMethod.GET).setURI(URI_VALUE).build();

  @BeforeEach
  void setUp() {
    given(this.client.method(HttpMethod.GET)).willReturn(this.request);
    given(this.request.uri(URI_VALUE)).willReturn(this.request);
  }

  @Test
  void retrievesHeadersAndBodilessEntity() {
    var response = mock(RestClient.ResponseSpec.class);
    ResponseEntity<Void> entity = ResponseEntity.noContent().header("X-Trace", "123").build();
    given(this.request.retrieve()).willReturn(response);
    given(response.toBodilessEntity()).willReturn(entity);

    assertThat(this.adapter.exchangeForHeaders(this.values).getFirst("X-Trace")).isEqualTo("123");
    assertThat(this.adapter.exchangeForBodilessEntity(this.values)).isSameAs(entity);
  }

  @Test
  void usesConfiguredExecutorForAsyncBodyAndEntityExchanges() {
    var response = mock(RestClient.AsyncSpec.class);
    var body = Future.ok("body");
    var empty = Future.<Void>ok();
    var entity = Future.ok(ResponseEntity.ok("body"));
    var bodiless = Future.ok(ResponseEntity.noContent().<Void>build());
    given(this.request.async(this.executor)).willReturn(response);
    given(response.body(BODY_TYPE)).willReturn(body);
    given(response.toBodiless()).willReturn(empty);
    given(response.toEntity(BODY_TYPE)).willReturn(entity);
    given(response.toBodilessEntity()).willReturn(bodiless);

    assertThat(this.adapter.exchangeAsyncBody(this.values, BODY_TYPE)).isSameAs(body);
    assertThat(this.adapter.exchangeAsyncVoid(this.values)).isSameAs(empty);
    assertThat(this.adapter.exchangeForEntityAsync(this.values, BODY_TYPE)).isSameAs(entity);
    assertThat(this.adapter.exchangeForBodilessEntityAsync(this.values)).isSameAs(bodiless);
  }

  @ParameterizedTest
  @ValueSource(strings = { "inputMessage", "httpResponse", "clientResponse" })
  void exposesRawResponsesWithoutClosingThem(String methodName) throws Exception {
    var response = mock(ClientResponse.class);
    given(this.request.execute(false)).willReturn(response);
    var method = ReturnTypes.class.getMethod(methodName);
    var execution = this.adapter.createRequestExecution(method, new MethodParameter(method, -1), false);

    assertThat(execution).isNotNull();
    assertThat(execution.execute(this.values)).isSameAs(response);
  }

  @Test
  void exposesRawResponsesAsFuturesUsingConfiguredExecutor() throws Exception {
    var response = Future.ok(mock(ClientResponse.class));
    given(this.request.send(this.executor)).willReturn(response);
    var method = ReturnTypes.class.getMethod("asyncResponse");
    var returnType = new MethodParameter(method, -1).nested();
    var execution = this.adapter.createRequestExecution(method, returnType, true);

    assertThat(execution).isNotNull();
    assertThat(execution.execute(this.values)).isSameAs(response);
  }

  @Test
  void rejectsRequestValuesWithoutUriOrTemplate() {
    var incomplete = mock(HttpRequestValues.class);
    given(incomplete.getHttpMethod()).willReturn(HttpMethod.GET);

    assertThatIllegalStateException().isThrownBy(() -> this.adapter.newRequest(incomplete))
            .withMessage("Neither full URL nor URI template");
  }

  private interface ReturnTypes {

    HttpInputMessage inputMessage();

    ClientHttpResponse httpResponse();

    ClientResponse clientResponse();

    Future<ClientResponse> asyncResponse();
  }

}
