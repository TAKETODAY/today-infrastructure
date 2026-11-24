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

package infra.mock.http.client.reactive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MockClientHttpResponseTests {

  @Test
  void statusAndEmptyBody() {
    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.CREATED);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getRawStatusCode()).isEqualTo(201);
    assertThat(response.getHeaders().isEmpty()).isTrue();
    assertThat(response.getCookies()).isEmpty();
    assertThat(response.getBodyAsString().block()).isEmpty();
    assertThat(response.toString()).startsWith("CREATED(201)");
    assertThat(new MockClientHttpResponse(599).toString()).startsWith("Status (599)");
  }

  @ParameterizedTest
  @ValueSource(ints = { 99, 1000 })
  void rejectsOutOfRangeStatus(int status) {
    assertThatIllegalArgumentException().isThrownBy(() -> new MockClientHttpResponse(status))
            .withMessage("Status must be between 100 and 999");
  }

  @Test
  void acceptsBoundaryStatusAndRejectsNull() {
    assertThat(new MockClientHttpResponse(100).getRawStatusCode()).isEqualTo(100);
    assertThat(new MockClientHttpResponse(999).getRawStatusCode()).isEqualTo(999);
    assertThatIllegalArgumentException().isThrownBy(() -> new MockClientHttpResponse(null))
            .withMessage("HttpStatus is required");
  }

  @Test
  void cookiesAreRenderedOnlyOnceAndDoNotReplaceExplicitHeaders() {
    MockClientHttpResponse response = new MockClientHttpResponse(200);
    ResponseCookie cookie = ResponseCookie.builder("session", "123").path("/").build();
    response.getCookies().add("session", cookie);
    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).containsExactly(cookie.toString());
    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).containsExactly(cookie.toString());

    MockClientHttpResponse explicit = new MockClientHttpResponse(200);
    explicit.getHeaders().add(HttpHeaders.SET_COOKIE, "explicit=value");
    explicit.getCookies().add("session", cookie);
    assertThat(explicit.getHeaders().get(HttpHeaders.SET_COOKIE)).containsExactly("explicit=value");
  }

  @Test
  void bodyUsesUtf8WithAndWithoutContentType() {
    MockClientHttpResponse response = new MockClientHttpResponse(200);
    response.setBody("你好");
    assertThat(response.getBodyAsString().block()).isEqualTo("你好");
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.setBody("café");
    assertThat(response.getBodyAsString().block()).isEqualTo("café");
  }

  @Test
  void bodyUsesExplicitCharsetAndPublisher() {
    MockClientHttpResponse response = new MockClientHttpResponse(200);
    response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    response.setBody("café", StandardCharsets.ISO_8859_1);
    assertThat(response.getBodyAsString().block()).isEqualTo("café");
    response.setBody(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(new byte[] { 'a', 'b' })));
    assertThat(response.getBodyAsString().block()).isEqualTo("ab");
  }

  @Test
  void publisherFailureIsPropagated() {
    MockClientHttpResponse response = new MockClientHttpResponse(200);
    IllegalStateException failure = new IllegalStateException("body failed");
    response.setBody(Flux.error(failure));
    StepVerifier.create(response.getBodyAsString()).expectErrorSatisfies(ex ->
            assertThat(ex).isSameAs(failure)).verify();
  }

}
