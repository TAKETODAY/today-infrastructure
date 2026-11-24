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

package infra.http.reactive.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class ReactorHttpHandlerAdapterTests {

  private final HttpServerRequest nativeRequest = mock(HttpServerRequest.class);

  private final HttpServerResponse nativeResponse = mock(HttpServerResponse.class);

  private final DefaultHttpHeaders responseHeaders = new DefaultHttpHeaders();

  @BeforeEach
  void setUp() {
    given(nativeRequest.method()).willReturn(io.netty.handler.codec.http.HttpMethod.GET);
    given(nativeRequest.requestHeaders()).willReturn(new DefaultHttpHeaders());
    given(nativeRequest.scheme()).willReturn("http");
    given(nativeRequest.hostName()).willReturn("example.com");
    given(nativeRequest.hostPort()).willReturn(80);
    given(nativeResponse.alloc()).willReturn(ByteBufAllocator.DEFAULT);
    given(nativeResponse.responseHeaders()).willReturn(responseHeaders);
  }

  @Test
  void adaptsRequestAndCommitsResponse() {
    given(nativeRequest.uri()).willReturn("/items?page=2");
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter((request, response) -> {
      assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
      assertThat(request.getURI()).isEqualTo(URI.create("http://example.com/items?page=2"));
      assertThat(request.getQueryParams().getFirst("page")).isEqualTo("2");
      response.setStatusCode(HttpStatus.ACCEPTED);
      response.getHeaders().set("X-Result", "accepted");
      return response.setComplete();
    });

    StepVerifier.create(adapter.apply(nativeRequest, nativeResponse)).expectComplete().verify(Duration.ofSeconds(5));

    then(nativeResponse).should().status(202);
    assertThat(responseHeaders.get("X-Result")).isEqualTo("accepted");
  }

  @Test
  void headRequestSetsContentLengthWithoutSendingBody() {
    given(nativeRequest.method()).willReturn(io.netty.handler.codec.http.HttpMethod.HEAD);
    given(nativeRequest.uri()).willReturn("/items");
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter((request, response) -> {
      assertThat(request.getMethod()).isEqualTo(HttpMethod.HEAD);
      return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(new byte[] { 1, 2, 3 })));
    });

    StepVerifier.create(adapter.apply(nativeRequest, nativeResponse)).expectComplete().verify(Duration.ofSeconds(5));

    assertThat(responseHeaders.get("Content-Length")).isEqualTo("3");
    then(nativeResponse).should(never()).send(any());
  }

  @Test
  void malformedUriIsRejectedWithoutInvokingHandler() {
    given(nativeRequest.uri()).willReturn("/incomplete%2");
    HttpHandler handler = mock(HttpHandler.class);
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);

    StepVerifier.create(adapter.apply(nativeRequest, nativeResponse)).expectComplete().verify(Duration.ofSeconds(5));

    then(nativeResponse).should().status(HttpResponseStatus.BAD_REQUEST);
    then(handler).shouldHaveNoInteractions();
  }

  @Test
  void propagatesHandlerFailure() {
    given(nativeRequest.uri()).willReturn("/");
    IllegalStateException failure = new IllegalStateException("handler failed");
    ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter((request, response) -> Mono.error(failure));

    StepVerifier.create(adapter.apply(nativeRequest, nativeResponse))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify(Duration.ofSeconds(5));
  }

}
