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

package infra.http.reactive.server;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.json.JacksonJsonEncoder;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractServerHttpRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
public class ServerHttpResponseTests {

  @Test
  void writeWith() {
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

    assertThat(response.statusCodeWritten).isTrue();
    assertThat(response.headersWritten).isTrue();
    assertThat(response.cookiesWritten).isTrue();

    assertThat(response.body).hasSize(3);
    assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("a");
    assertThat(response.body.get(1).toString(StandardCharsets.UTF_8)).isEqualTo("b");
    assertThat(response.body.get(2).toString(StandardCharsets.UTF_8)).isEqualTo("c");
  }

  @Test
    //
  void writeAndFlushWithFluxOfDefaultDataBuffer() {
    TestServerHttpResponse response = new TestServerHttpResponse();
    Flux<Flux<DefaultDataBuffer>> flux = Flux.just(Flux.just(wrap("foo")));
    response.writeAndFlushWith(flux).block();

    assertThat(response.statusCodeWritten).isTrue();
    assertThat(response.headersWritten).isTrue();
    assertThat(response.cookiesWritten).isTrue();

    assertThat(response.body).hasSize(1);
    assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("foo");
  }

  @Test
  void writeWithFluxError() {
    IllegalStateException error = new IllegalStateException("boo");
    writeWithError(Flux.error(error));
  }

  @Test
  void writeWithMonoError() {
    IllegalStateException error = new IllegalStateException("boo");
    writeWithError(Mono.error(error));
  }

  void writeWithError(Publisher<DataBuffer> body) {
    TestServerHttpResponse response = new TestServerHttpResponse();
    HttpHeaders headers = response.getHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setOrRemove(HttpHeaders.CONTENT_ENCODING, "gzip");
    headers.setContentLength(12);
    response.writeWith(body).onErrorComplete().block();

    assertThat(response.statusCodeWritten).isFalse();
    assertThat(response.headersWritten).isFalse();
    assertThat(response.cookiesWritten).isFalse();
    assertThat(headers.asMultiValueMap()).doesNotContainKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.CONTENT_ENCODING);
    assertThat(response.body.isEmpty()).isTrue();
  }

  @Test
  void setComplete() {
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.setComplete().block();

    assertThat(response.statusCodeWritten).isTrue();
    assertThat(response.headersWritten).isTrue();
    assertThat(response.cookiesWritten).isTrue();
    assertThat(response.body.isEmpty()).isTrue();
  }

  @Test
  void statusAndCookiesCannotBeChangedAfterCommit() {
    TestServerHttpResponse response = new TestServerHttpResponse();
    ResponseCookie cookie = ResponseCookie.forSimple("theme", "dark");
    assertThat(response.setStatusCode(HttpStatus.CREATED)).isTrue();
    assertThat(response.setStatusCode(null)).isTrue();
    assertThat(response.getRawStatusCode()).isNull();
    assertThat(response.setRawStatusCode(299)).isTrue();
    response.beforeCommit(() -> Mono.fromRunnable(() -> {
      response.addCookie(cookie);
      response.getHeaders().set("X-Result", "sent");
    }));

    StepVerifier.create(response.doCommit()).expectComplete().verify(Duration.ofSeconds(5));
    StepVerifier.create(response.doCommit()).expectComplete().verify(Duration.ofSeconds(5));
    StepVerifier.create(response.setComplete()).expectComplete().verify(Duration.ofSeconds(5));

    assertThat(response.isCommitted()).isTrue();
    assertThat(response.setStatusCode(HttpStatus.OK)).isFalse();
    assertThat(response.setRawStatusCode(200)).isFalse();
    assertThat(response.getRawStatusCode()).isEqualTo(299);
    assertThat(response.getCookies().get("theme")).containsExactly(cookie);
    assertThatIllegalStateException().isThrownBy(() -> response.addCookie(cookie));
    assertThatThrownBy(() -> response.getCookies().add("theme", cookie))
            .isInstanceOf(UnsupportedOperationException.class);
    HttpHeaders committedHeaders = response.getHeaders();
    assertThat(committedHeaders.getFirst("X-Result")).isEqualTo("sent");
    assertThat(response.getHeaders()).isSameAs(committedHeaders);
    assertThatThrownBy(() -> committedHeaders.set("X-Result", "changed"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void defaultRawStatusMethodsAdaptStandardStatusCodes() {
    ServerHttpResponse response = mock(ServerHttpResponse.class, CALLS_REAL_METHODS);
    given(response.setStatusCode(HttpStatus.ACCEPTED)).willReturn(true);
    given(response.getStatusCode()).willReturn(null, HttpStatus.ACCEPTED);

    assertThat(response.getRawStatusCode()).isNull();
    assertThat(response.setRawStatusCode(202)).isTrue();
    assertThat(response.getRawStatusCode()).isEqualTo(202);
    assertThat(response.setRawStatusCode(null)).isFalse();
    then(response).should().setStatusCode(null);
    assertThatIllegalStateException().isThrownBy(() -> response.setRawStatusCode(299))
            .withMessageContaining("299");
  }

  @Test
  void releasesMonoBodyWhenCancelledBeforeWriting() {
    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.beforeCommit(Mono::never);

    StepVerifier.create(response.writeWith(Mono.fromSupplier(bufferFactory::allocateBuffer)))
            .then(() -> assertThat(response.body).isEmpty())
            .thenCancel().verify(Duration.ofSeconds(5));

    assertThat(response.headersWritten).isFalse();
    bufferFactory.checkForLeaks();
  }

  @Test
  void flushSourceFailureClearsContentHeadersWithoutCommitting() {
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.getHeaders().setContentLength(10);
    response.getHeaders().set("X-Result", "preserved");
    IllegalStateException failure = new IllegalStateException("flush source failed");

    StepVerifier.create(response.writeAndFlushWith(Flux.error(failure)))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify(Duration.ofSeconds(5));

    assertThat(response.isCommitted()).isFalse();
    assertThat(response.getHeaders().asMultiValueMap()).doesNotContainKeys(HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_LENGTH);
    assertThat(response.getHeaders().getFirst("X-Result")).isEqualTo("preserved");
  }

  @Test
  void beforeCommitWithComplete() {
    ResponseCookie cookie = ResponseCookie.builder("ID", "123").build();
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.beforeCommit(() -> Mono.fromRunnable(() -> response.getCookies().add(cookie.getName(), cookie)));
    response.writeWith(Flux.just(wrap("a"), wrap("b"), wrap("c"))).block();

    assertThat(response.statusCodeWritten).isTrue();
    assertThat(response.headersWritten).isTrue();
    assertThat(response.cookiesWritten).isTrue();
    assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);

    assertThat(response.body).hasSize(3);
    assertThat(response.body.get(0).toString(StandardCharsets.UTF_8)).isEqualTo("a");
    assertThat(response.body.get(1).toString(StandardCharsets.UTF_8)).isEqualTo("b");
    assertThat(response.body.get(2).toString(StandardCharsets.UTF_8)).isEqualTo("c");
  }

  @Test
  void beforeCommitActionWithSetComplete() {
    ResponseCookie cookie = ResponseCookie.builder("ID", "123").build();
    TestServerHttpResponse response = new TestServerHttpResponse();
    response.beforeCommit(() -> {
      response.getCookies().add(cookie.getName(), cookie);
      return Mono.empty();
    });
    response.setComplete().block();

    assertThat(response.statusCodeWritten).isTrue();
    assertThat(response.headersWritten).isTrue();
    assertThat(response.cookiesWritten).isTrue();
    assertThat(response.body.isEmpty()).isTrue();
    assertThat(response.getCookies().getFirst("ID")).isSameAs(cookie);
  }

  @Test
    // gh-24186, gh-25753
  void beforeCommitErrorShouldLeaveResponseNotCommitted() {

    Consumer<Supplier<Mono<Void>>> tester = preCommitAction -> {
      TestServerHttpResponse response = new TestServerHttpResponse();
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      response.getHeaders().setContentLength(3);
      response.beforeCommit(preCommitAction);

      StepVerifier.create(response.writeWith(Flux.just(wrap("body"))))
              .expectErrorMessage("Max sessions")
              .verify();

      assertThat(response.statusCodeWritten).isFalse();
      assertThat(response.headersWritten).isFalse();
      assertThat(response.cookiesWritten).isFalse();
      assertThat(response.isCommitted()).isFalse();
      assertThat(response.getHeaders().isEmpty()).isTrue();

      // Handle the error
      response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
      StepVerifier.create(response.setComplete()).verifyComplete();

      assertThat(response.statusCodeWritten).isTrue();
      assertThat(response.headersWritten).isTrue();
      assertThat(response.cookiesWritten).isTrue();
      assertThat(response.isCommitted()).isTrue();
    };

    tester.accept(() -> Mono.error(new IllegalStateException("Max sessions")));
    tester.accept(() -> {
      throw new IllegalStateException("Max sessions");
    });
  }

  @Test
    // gh-26232
  void monoResponseShouldNotLeakIfCancelled() {
    LeakAwareDataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();
    MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
    MockServerHttpResponse response = new MockServerHttpResponse(bufferFactory);
    response.setWriteHandler(flux -> {
      throw AbortedException.beforeSend();
    });

    HttpMessageWriter<Object> messageWriter = new EncoderHttpMessageWriter<>(new JacksonJsonEncoder());
    Mono<Void> result = messageWriter.write(Mono.just(Collections.singletonMap("foo", "bar")),
            ResolvableType.forClass(Mono.class), ResolvableType.forClass(Map.class), null,
            request, response, Collections.emptyMap());

    StepVerifier.create(result).expectError(AbortedException.class).verify();

    bufferFactory.checkForLeaks();
  }

  private DefaultDataBuffer wrap(String a) {
    return DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(a.getBytes(StandardCharsets.UTF_8)));
  }

  private static class TestServerHttpResponse extends AbstractServerHttpResponse {

    private boolean statusCodeWritten;

    private boolean headersWritten;

    private boolean cookiesWritten;

    private final List<DataBuffer> body = new ArrayList<>();

    public TestServerHttpResponse() {
      super(DefaultDataBufferFactory.sharedInstance);
    }

    @Override
    public <T> T getNativeResponse() {
      throw new IllegalStateException("This is a mock. No running server, no native response.");
    }

    @Override
    public void applyStatusCode() {
      assertThat(this.statusCodeWritten).isFalse();
      this.statusCodeWritten = true;
    }

    @Override
    protected void applyHeaders() {
      assertThat(this.headersWritten).isFalse();
      this.headersWritten = true;
    }

    @Override
    protected void applyCookies() {
      assertThat(this.cookiesWritten).isFalse();
      this.cookiesWritten = true;
    }

    @Override
    protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
      return Flux.from(body).map(b -> {
        this.body.add(b);
        return b;
      }).then();
    }

    @Override
    protected Mono<Void> writeAndFlushWithInternal(
            Publisher<? extends Publisher<? extends DataBuffer>> bodyWithFlush) {
      return Flux.from(bodyWithFlush).flatMap(body ->
              Flux.from(body).map(b -> {
                this.body.add(b);
                return b;
              })
      ).then();
    }
  }

}
