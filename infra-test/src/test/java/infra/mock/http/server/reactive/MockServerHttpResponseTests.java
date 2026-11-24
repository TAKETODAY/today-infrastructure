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

package infra.mock.http.server.reactive;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MockServerHttpResponseTests {

  @Test
  void unwrittenBodyAndNativeResponseAreUnavailable() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    StepVerifier.create(response.getBodyAsString()).expectErrorMessage(
            "No content was written nor was setComplete() called on this response.").verify();
    assertThatIllegalStateException().isThrownBy(response::getNativeResponse)
            .withMessage("This is a mock. No running server, no native response.");
    assertThatIllegalArgumentException().isThrownBy(() -> response.setWriteHandler(null))
            .withMessage("'writeHandler' is required");
  }

  @Test
  void writeCommitsStatusCookiesAndCachesUtf8Body() {
    DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
    MockServerHttpResponse response = new MockServerHttpResponse(factory);
    assertThat(response.bufferFactory()).isSameAs(factory);
    response.setStatusCode(HttpStatus.CREATED);
    ResponseCookie cookie = ResponseCookie.builder("session", "value").build();
    response.getCookies().add("session", cookie);
    AtomicInteger subscriptions = new AtomicInteger();
    response.writeWith(Flux.defer(() -> {
      subscriptions.incrementAndGet();
      return Flux.just(factory.wrap("你好".getBytes(StandardCharsets.UTF_8)));
    })).block();
    assertThat(response.isCommitted()).isTrue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).containsExactly(cookie.toString());
    assertThat(response.getBodyAsString().block()).isEqualTo("你好");
    assertThat(response.getBodyAsString().block()).isEqualTo("你好");
    assertThat(subscriptions).hasValue(1);
  }

  @Test
  void flushPreservesOrderAndDecodesConfiguredCharset() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    response.writeAndFlushWith(Flux.just(
            Mono.just(response.bufferFactory().wrap(new byte[] { (byte) 0xe9 })),
            Mono.just(response.bufferFactory().wrap(new byte[] { '!' })))).block();
    assertThat(response.getBodyAsString().block()).isEqualTo("é!");
  }

  @Test
  void completeProducesEmptyBody() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.setComplete().block();
    assertThat(response.getBodyAsString().block()).isEmpty();
    StepVerifier.create(response.getBody()).verifyComplete();
  }

  @Test
  void customHandlerConsumesBodyWithoutCachingIt() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    AtomicInteger count = new AtomicInteger();
    response.setWriteHandler(body -> body.doOnNext(buffer -> {
      count.addAndGet(buffer.readableBytes());
      buffer.release();
    }).then());
    response.writeWith(Mono.just(response.bufferFactory().wrap(new byte[] { 1, 2 }))).block();
    assertThat(count).hasValue(2);
    StepVerifier.create(response.getBody()).expectErrorMessage("Not available with custom write handler.").verify();
  }

  @Test
  void writePropagatesPublisherFailure() {
    MockServerHttpResponse response = new MockServerHttpResponse();
    IllegalStateException failure = new IllegalStateException("body failed");
    StepVerifier.create(response.writeWith(Flux.concat(
            Mono.just(response.bufferFactory().wrap(new byte[] { 1 })), Flux.error(failure))))
            .expectErrorSatisfies(ex -> assertThat(ex).isSameAs(failure)).verify();
    StepVerifier.create(response.getBody())
            .assertNext(buffer -> {
              assertThat(buffer.readableBytes()).isEqualTo(1);
              buffer.release();
            })
            .expectErrorSatisfies(ex -> assertThat(ex).isSameAs(failure)).verify();
  }

}
