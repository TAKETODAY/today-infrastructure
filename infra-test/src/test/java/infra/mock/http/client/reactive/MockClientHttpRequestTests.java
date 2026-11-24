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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MockClientHttpRequestTests {

  @Test
  void requestMetadataAndBodyBeforeWriting() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
            "https://example.org/{item}", "a b");
    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(request.getURI()).isEqualTo(URI.create("https://example.org/a%20b"));
    assertThat(request.<Object>getNativeRequest()).isSameAs(request);
    StepVerifier.create(request.getBodyAsString())
            .expectErrorMatches(ex -> ex instanceof IllegalStateException && ex.getMessage().contains("body is not set"))
            .verify();
  }

  @Test
  void writeCommitsCookiesAndCachesBody() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.PUT, URI.create("/"));
    request.getCookies().add("session", new HttpCookie("session", "123"));
    AtomicInteger subscriptions = new AtomicInteger();
    Flux<DataBuffer> body = Flux.defer(() -> {
      subscriptions.incrementAndGet();
      return Flux.just(request.bufferFactory().wrap("你好".getBytes(StandardCharsets.UTF_8)));
    });
    request.writeWith(body).block();
    assertThat(request.isCommitted()).isTrue();
    assertThat(request.getHeaders().get(HttpHeaders.COOKIE)).containsExactly("session=123");
    assertThat(request.getBodyAsString().block()).isEqualTo("你好");
    assertThat(request.getBodyAsString().block()).isEqualTo("你好");
    assertThat(subscriptions).hasValue(1);
  }

  @Test
  void writeAndFlushUsesContentTypeCharset() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));
    request.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    request.writeAndFlushWith(Flux.just(
            Mono.just(request.bufferFactory().wrap(new byte[] { (byte) 0xe9 })),
            Mono.just(request.bufferFactory().wrap(new byte[] { '!' })))).block();
    assertThat(request.getBodyAsString().block()).isEqualTo("é!");
  }

  @Test
  void completeWritesEmptyBody() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
    request.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    request.setComplete().block();
    assertThat(request.getBodyAsString().block()).isEmpty();
    StepVerifier.create(request.getBody()).verifyComplete();
  }

  @Test
  void customHandlerConsumesBodyWithoutMakingItAvailable() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));
    AtomicInteger bytes = new AtomicInteger();
    request.setWriteHandler(body -> body.doOnNext(buffer -> {
      bytes.addAndGet(buffer.readableBytes());
      buffer.release();
    }).then());
    request.writeWith(Mono.just(request.bufferFactory().wrap(new byte[] { 1, 2 }))).block();
    assertThat(bytes).hasValue(2);
    StepVerifier.create(request.getBody()).expectError(IllegalStateException.class).verify();
    assertThatIllegalArgumentException().isThrownBy(() -> request.setWriteHandler(null))
            .withMessage("'writeHandler' is required");
  }

  @Test
  void bodyFailureIsPropagated() {
    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));
    IllegalStateException failure = new IllegalStateException("failed body");
    StepVerifier.create(request.writeWith(Flux.error(failure))).expectErrorSatisfies(ex ->
            assertThat(ex).isSameAs(failure)).verify();
  }

}
