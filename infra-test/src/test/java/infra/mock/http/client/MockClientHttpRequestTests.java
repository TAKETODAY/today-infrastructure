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

package infra.mock.http.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MockClientHttpRequestTests {

  @Test
  void defaultsCanBeOverridden() {
    MockClientHttpRequest request = new MockClientHttpRequest();
    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(request.getURI()).isEqualTo(URI.create("/"));
    assertThat(request.isExecuted()).isFalse();
    assertThat(request.toString()).isEqualTo("GET /");

    request.setMethod(HttpMethod.PATCH);
    request.setURI(URI.create("/updated"));
    request.getHeaders().set("X-Test", "value");
    assertThat(request.getMethod()).isEqualTo(HttpMethod.PATCH);
    assertThat(request.getMethodAsString()).isEqualTo("PATCH");
    assertThat(request.getURI()).isEqualTo(URI.create("/updated"));
    assertThat(request.toString()).contains("PATCH /updated", "X-Test", "value");
  }

  @Test
  void expandsAndEncodesUriTemplate() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
            "https://example.org/items/{name}", "a b");
    assertThat(request.getURI()).isEqualTo(URI.create("https://example.org/items/a%20b"));
    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
  }

  @Test
  void executeReturnsConfiguredResponse() throws IOException {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/items"));
    MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
    request.setResponse(response);
    assertThat(request.execute()).isSameAs(response);
    assertThat(request.isExecuted()).isTrue();
  }

  @Test
  void executeWithoutResponseStillMarksRequestExecuted() {
    MockClientHttpRequest request = new MockClientHttpRequest();
    assertThatIllegalStateException().isThrownBy(request::execute).withMessage("No ClientHttpResponse");
    assertThat(request.isExecuted()).isTrue();
  }

  @Test
  void executeUsesSubclassResponseAndPropagatesFailure() {
    IOException failure = new IOException("offline");
    MockClientHttpRequest request = new MockClientHttpRequest() {
      @Override
      protected ClientHttpResponse executeInternal() throws IOException {
        throw failure;
      }
    };
    assertThatExceptionOfType(IOException.class).isThrownBy(request::execute).isSameAs(failure);
    assertThat(request.isExecuted()).isTrue();
  }

  @Test
  void asyncUsesSuppliedExecutor() throws Exception {
    MockClientHttpRequest request = new MockClientHttpRequest();
    MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], 204);
    request.setResponse(response);
    AtomicBoolean invoked = new AtomicBoolean();
    Executor executor = task -> {
      invoked.set(true);
      task.run();
    };
    assertThat(request.async(executor).get()).isSameAs(response);
    assertThat(invoked).isTrue();
    assertThat(request.isExecuted()).isTrue();
  }

}
