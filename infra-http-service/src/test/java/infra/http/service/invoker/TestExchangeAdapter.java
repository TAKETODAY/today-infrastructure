/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;

import java.util.Collections;

import infra.core.ParameterizedTypeReference;
import infra.http.ResponseEntity;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import infra.web.testfixture.http.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
public class TestExchangeAdapter implements HttpExchangeAdapter {

  @Nullable
  private String invokedMethodName;

  @Nullable
  private HttpRequestValues requestValues;

  @Nullable
  private ParameterizedTypeReference<?> bodyType;

  public String getInvokedMethodName() {
    assertThat(this.invokedMethodName).isNotNull();
    return this.invokedMethodName;
  }

  public HttpRequestValues getRequestValues() {
    assertThat(this.requestValues).isNotNull();
    return this.requestValues;
  }

  @Nullable
  public ParameterizedTypeReference<?> getBodyType() {
    return this.bodyType;
  }

  @Override
  public ClientResponse exchange(HttpRequestValues requestValues) {
    saveInput("exchange", requestValues, null);
    return new MockClientHttpResponse();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("exchangeForBody", requestValues, bodyType);
    return bodyType.getType().getTypeName().contains("List") ?
            (T) Collections.singletonList(getInvokedMethodName()) : (T) getInvokedMethodName();
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
    saveInput("exchangeForBodilessEntity", requestValues, null);
    return ResponseEntity.ok().build();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ResponseEntity<T> exchangeForEntity(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    saveInput("exchangeForEntity", requestValues, bodyType);
    return (ResponseEntity<T>) ResponseEntity.ok(getInvokedMethodName());
  }

  @Override
  public Future<ClientResponse> exchangeAsync(HttpRequestValues requestValues) {
    saveInput("exchangeAsync", requestValues, null);
    MockClientHttpResponse result = new MockClientHttpResponse();
    return Future.ok(result);
  }

  @Override
  public <T> Future<T> exchangeAsyncBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyTypeRef) {
    saveInput("exchangeAsyncBody", requestValues, bodyTypeRef);
    return Future.ok();
  }

  @Override
  public Future<Void> exchangeAsyncVoid(HttpRequestValues requestValues) {
    saveInput("exchangeAsyncVoid", requestValues, null);
    return Future.ok();
  }

  @Override
  public <T> Future<ResponseEntity<T>> exchangeForEntityAsync(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return Future.ok(exchangeForEntity(requestValues, bodyType));
  }

  @Override
  public Future<ResponseEntity<Void>> exchangeForBodilessEntityAsync(HttpRequestValues requestValues) {
    return Future.ok(exchangeForBodilessEntity(requestValues));
  }

  @Override
  public boolean supportsRequestAttributes() {
    return true;
  }

  protected <T> void saveInput(
          String methodName, HttpRequestValues values, @Nullable ParameterizedTypeReference<T> bodyType) {

    this.invokedMethodName = methodName;
    this.requestValues = values;
    this.bodyType = bodyType;
  }

}
