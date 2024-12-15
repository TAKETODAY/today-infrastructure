/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.http.server.reactive;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import infra.http.codec.json.Jackson2JsonEncoder;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(headers).doesNotContainKeys(HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH,
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
  void beforeCommitWithComplete() {
    ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
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
    ResponseCookie cookie = ResponseCookie.from("ID", "123").build();
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
      assertThat(response.getHeaders()).isEmpty();

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

    HttpMessageWriter<Object> messageWriter = new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder());
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
