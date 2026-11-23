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

package infra.http.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.FlowAdapters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import reactor.core.publisher.Flux;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for request execution and header handling in {@link JdkClientHttpRequest}.
 */
class JdkClientHttpRequestTests {

  private static final URI URI_VALUE = URI.create("https://example.test/");

  @ParameterizedTest
  @ValueSource(strings = { "gzip", "deflate", "identity", "" })
  void decodesSupportedEncodingsAndPreservesOtherResponseHeaders(String encoding) throws Exception {
    byte[] payload = "response body".getBytes(UTF_8);
    byte[] wireBody = encode(encoding, payload);
    boolean compressed = encoding.equals("gzip") || encoding.equals("deflate");
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation -> {
      HttpRequest sent = invocation.getArgument(0);
      assertThat(sent.method()).isEqualTo("DELETE");
      assertThat(sent.headers().allValues(HttpHeaders.ACCEPT_ENCODING)).containsExactly("gzip", "deflate");
      return respond(invocation.getArgument(1), encoding, wireBody);
    });

    var request = new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.DELETE, Runnable::run, null, true);
    try (var response = request.execute()) {
      assertThat(response.getBody().readAllBytes()).containsExactly(payload);
      assertThat(response.getHeaders().contains(HttpHeaders.CONTENT_ENCODING)).isEqualTo(!compressed && !encoding.isEmpty());
      assertThat(response.getHeaders().getContentLength()).isEqualTo(compressed ? -1 : wireBody.length);
    }
  }

  @Test
  void keepsExplicitAcceptEncodingAndHandlesAnEmptyCompressedResponseAsynchronously() throws Exception {
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation -> {
      HttpRequest sent = invocation.getArgument(0);
      assertThat(sent.headers().allValues(HttpHeaders.ACCEPT_ENCODING)).containsExactly("gzip");
      return respond(invocation.getArgument(1), "gzip", new byte[0]);
    });
    var request = new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.GET, Runnable::run, Duration.ofSeconds(10), true);
    request.getHeaders().set(HttpHeaders.ACCEPT_ENCODING, "gzip");

    try (var response = request.async(Runnable::run).get(5, TimeUnit.SECONDS)) {
      assertThat(response.getBody().readAllBytes()).isEmpty();
      assertThat(response.getHeaders().contains(HttpHeaders.CONTENT_ENCODING)).isFalse();
    }
  }

  @Test
  void reportsInvalidGzipDataAsAnIOException() {
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation ->
            respond(invocation.getArgument(1), "gzip", "not gzip data".getBytes(UTF_8)));
    var request = new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.GET, Runnable::run, null, true);

    assertThatIOException().isThrownBy(request::execute).withMessage("Not in GZIP format");
  }

  @Test
  void returnsTheUnderlyingBodyWithAReadTimeoutAndClosesIt() throws Exception {
    InputStream body = mock();
    given(body.read()).willReturn(42);
    given(body.read(any(byte[].class), anyInt(), anyInt())).willReturn(-1);
    HttpResponse<InputStream> response = responseWithHeaders(Map.of());
    given(response.body()).willReturn(body);
    var request = requestWith(CompletableFuture.completedFuture(response), Duration.ofMinutes(1));

    try (var result = request.execute()) {
      assertThat(result.getBody().read()).isEqualTo(42);
    }
    verify(body).close();
  }

  @Test
  void usesAnEmptyStreamWhenTimedResponseHasNoBody() throws Exception {
    HttpResponse<InputStream> response = responseWithHeaders(Map.of());
    var request = requestWith(CompletableFuture.completedFuture(response), Duration.ofMinutes(1));

    try (var result = request.execute()) {
      assertThat(result.getBody().read()).isEqualTo(-1);
    }
  }

  @Test
  void cancelsAnExchangeThatDoesNotCompleteBeforeItsReadTimeout() {
    var pending = new CompletableFuture<HttpResponse<InputStream>>();
    var request = requestWith(pending, Duration.ZERO);

    assertThatIOException().isThrownBy(request::execute).isInstanceOf(HttpTimeoutException.class);
    assertThat(pending).isCancelled();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void closesAnUnconsumedResponseBodyWhenTheTimeoutExpires(boolean closeFails) throws Exception {
    var closed = new CompletableFuture<Void>();
    HttpResponse<InputStream> response = responseWithHeaders(Map.of());
    given(response.body()).willReturn(new ByteArrayInputStream(new byte[] { 1 }) {
      @Override
      public void close() throws IOException {
        super.close();
        closed.complete(null);
        if (closeFails) {
          throw new IOException("close failed");
        }
      }
    });
    var request = requestWith(CompletableFuture.completedFuture(response), Duration.ZERO);

    try (var result = request.execute()) {
      closed.get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void doesNotConfuseTransportCancellationWithAReadTimeout() {
    var cancelled = new CompletableFuture<HttpResponse<InputStream>>();
    cancelled.cancel(true);
    var request = requestWith(cancelled, Duration.ofMinutes(1));

    assertThatIOException().isThrownBy(request::execute).withMessage("Request cancelled")
            .withCauseInstanceOf(CancellationException.class);
  }

  @Test
  void preservesTheTypeAndCauseOfTransportFailures() {
    var ioFailure = new IOException("transport failed");
    var runtimeFailure = new IllegalStateException("invalid exchange");
    var otherFailure = new Exception("unexpected failure");

    assertThatThrownBy(requestWith(CompletableFuture.failedFuture(ioFailure), null)::execute).isSameAs(ioFailure);
    assertThatThrownBy(requestWith(CompletableFuture.failedFuture(runtimeFailure), null)::execute).isSameAs(runtimeFailure);
    assertThatIOException().isThrownBy(requestWith(CompletableFuture.failedFuture(otherFailure), null)::execute)
            .withMessage("unexpected failure").withCause(otherFailure);
  }

  @Test
  void reportsCancellationWithAndWithoutAWrappedCause() {
    var cancellation = new CancellationException("transport cancelled");
    var wrapped = CompletableFuture.<HttpResponse<InputStream>>failedFuture(new CompletionException(cancellation));
    var direct = CompletableFuture.<HttpResponse<InputStream>>failedFuture(cancellation);

    assertThatIOException().isThrownBy(requestWith(wrapped, Duration.ofMinutes(1))::execute)
            .withMessage("Request cancelled").withCause(cancellation);
    assertThatIOException().isThrownBy(requestWith(wrapped, null)::execute)
            .withMessage("Request cancelled").withCause(cancellation);
    assertThatIOException().isThrownBy(requestWith(direct, null)::execute)
            .withMessage("Request cancelled").withCauseInstanceOf(CancellationException.class);
  }

  @Test
  void cancelsTransportAndRestoresTheInterruptFlagWhenWaitingIsInterrupted() {
    var pending = new CompletableFuture<HttpResponse<InputStream>>();
    var request = requestWith(pending, null);
    Thread.currentThread().interrupt();
    try {
      assertThatIOException().isThrownBy(request::execute).withCauseInstanceOf(InterruptedException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      assertThat(pending).isCancelled();
    }
    finally {
      Thread.interrupted();
    }
  }

  @Test
  void omitsAnExplicitlyEmptyBody() throws Exception {
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation -> {
      HttpRequest sent = invocation.getArgument(0);
      assertThat(sent.method()).isEqualTo("POST");
      assertThat(sent.bodyPublisher().orElseThrow().contentLength()).isZero();
      return respond(invocation.getArgument(1), "", new byte[0]);
    });
    var request = new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.POST, Runnable::run, null, false);
    request.setContentLength(0);
    request.setBody(output -> { throw new AssertionError("An empty body must not be written"); });

    try (var response = request.execute()) {
      assertThat(response.getBody().readAllBytes()).isEmpty();
    }
  }

  private static byte[] encode(String encoding, byte[] bytes) throws IOException {
    if (encoding.equals("gzip") || encoding.equals("deflate")) {
      var buffer = new ByteArrayOutputStream();
      try (var compressor = encoding.equals("gzip") ? new GZIPOutputStream(buffer) : new DeflaterOutputStream(buffer)) {
        compressor.write(bytes);
      }
      return buffer.toByteArray();
    }
    return bytes;
  }

  private static CompletableFuture<HttpResponse<InputStream>> respond(HttpResponse.BodyHandler<InputStream> handler,
          String encoding, byte[] bytes) {
    Map<String, List<String>> headers = encoding.isEmpty()
            ? Map.of(HttpHeaders.CONTENT_LENGTH, List.of(Integer.toString(bytes.length)))
            : Map.of(HttpHeaders.CONTENT_LENGTH, List.of(Integer.toString(bytes.length)), HttpHeaders.CONTENT_ENCODING, List.of(encoding));
    var response = responseWithHeaders(headers);
    HttpResponse.ResponseInfo info = mock();
    var wireHeaders = response.headers();
    given(info.headers()).willReturn(wireHeaders);
    var subscriber = handler.apply(info);
    subscriber.onSubscribe(mock(Flow.Subscription.class));
    if (bytes.length != 0) {
      subscriber.onNext(List.of(ByteBuffer.wrap(bytes)));
    }
    subscriber.onComplete();
    return subscriber.getBody().toCompletableFuture().thenApply(body -> {
      given(response.body()).willReturn(body);
      return response;
    });
  }

  private static HttpResponse<InputStream> responseWithHeaders(Map<String, List<String>> headers) {
    HttpResponse<InputStream> response = mock();
    given(response.headers()).willReturn(java.net.http.HttpHeaders.of(headers, (name, value) -> true));
    return response;
  }

  private static JdkClientHttpRequest requestWith(CompletableFuture<HttpResponse<InputStream>> outcome, Duration timeout) {
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willReturn(outcome);
    return new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.GET, Runnable::run, timeout, false);
  }

  @Test
  void requiresBothTransportAndExecutorWhenUsingAnExplicitExecutor() {
    HttpClient transport = mock();
    assertThatIllegalArgumentException().isThrownBy(() -> new JdkClientHttpRequestFactory(null, Runnable::run))
            .withMessage("HttpClient is required");
    assertThatIllegalArgumentException().isThrownBy(() -> new JdkClientHttpRequestFactory(transport, null))
            .withMessage("Executor is required");
  }

  @Test
  void usesTheFactoryExecutorToPublishTheRequestBody() throws Exception {
    HttpClient transport = mock();
    var writes = new AtomicInteger();
    var factory = new JdkClientHttpRequestFactory(transport, task -> {
      writes.incrementAndGet();
      task.run();
    });
    factory.setReadTimeout(0);
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation -> {
      HttpRequest sent = invocation.getArgument(0);
      var publisher = sent.bodyPublisher().orElseThrow();
      assertThat(publisher.contentLength()).isEqualTo(3);
      var buffers = Flux.from(FlowAdapters.toPublisher(publisher)).collectList().block(Duration.ofSeconds(5));
      assertThat(buffers).containsExactly(ByteBuffer.wrap(new byte[] { 2, 3, 5 }));
      return respond(invocation.getArgument(1), "", new byte[0]);
    });
    var request = factory.createRequest(URI_VALUE, HttpMethod.POST);
    request.setContentLength(3);
    request.getBody().write(new byte[] { 2, 3, 5 });

    try (var response = request.execute()) {
      assertThat(response.getBody().readAllBytes()).isEmpty();
    }
    assertThat(writes).hasValue(1);
  }

  @Test
  void preservesIntentionalEmptyHeaderValues() throws Exception {
    HttpClient transport = mock();
    given(transport.sendAsync(any(), any(HttpResponse.BodyHandler.class))).willAnswer(invocation -> {
      HttpRequest sent = invocation.getArgument(0);
      assertThat(sent.headers().allValues("X-Optional")).containsExactly("");
      return respond(invocation.getArgument(1), "", new byte[0]);
    });
    var request = new JdkClientHttpRequest(transport, URI_VALUE, HttpMethod.GET, Runnable::run, null, false);
    request.getHeaders().add("X-Optional", "");
    try (var response = request.execute()) {
      assertThat(response.getBody().read()).isEqualTo(-1);
    }
  }

  @Test
  void delegatesStatusAndBodyForKnownAndUnknownJdkStatuses() throws Exception {
    HttpResponse<InputStream> nativeResponse = mock();
    given(nativeResponse.statusCode()).willReturn(202, 599);
    var body = new ByteArrayInputStream(new byte[] { 1 });
    var headers = HttpHeaders.forWritable();
    var delegate = new JdkClientHttpResponse(nativeResponse, headers, body);
    try (var response = new DelegatingClientHttpResponse(delegate)) {
      assertThat(response.delegate()).isSameAs(delegate);
      assertThat(response.getStatusText()).isEqualTo("Accepted");
      assertThat(response.getStatusText()).isEmpty();
      assertThat(response.getBody()).isSameAs(body);
      assertThat(response.getHeaders()).isSameAs(headers);
    }
  }

  @Test
  void disallowedHeadersByDefault() {
    assertThat(disallowedHeaders(null))
            .containsExactlyInAnyOrder("connection", "content-length", "expect", "host", "upgrade");
  }

  @Test
  void disallowedHeadersWithSingleHeaderAllowed() {
    assertThat(disallowedHeaders("expect"))
            .containsExactlyInAnyOrder("connection", "content-length", "host", "upgrade");
  }

  @Test
  void disallowedHeadersWithMultipleHeadersAllowed() {
    assertThat(disallowedHeaders("expect,host"))
            .containsExactlyInAnyOrder("connection", "content-length", "upgrade");
  }

  @Test
  void disallowedHeadersAreCaseInsensitive() {
    assertThat(disallowedHeaders(null).contains("EXPECT")).isTrue();
    assertThat(disallowedHeaders("Expect").contains("expect")).isFalse();
  }

  @Test
  void disallowedHeadersRespectSystemProperty() {
    String original = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
    try {
      System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Expect,Host");
      Set<String> headers = JdkClientHttpRequest.disallowedHeaders();
      assertThat(headers).contains("connection", "content-length", "upgrade");
      assertThat(headers).doesNotContain("expect", "host");
    }
    finally {
      if (original == null) {
        System.clearProperty("jdk.httpclient.allowRestrictedHeaders");
      }
      else {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", original);
      }
    }
  }

  @Test
  void disallowedHeadersWithManyMixedCaseHeadersAllowed() {
    assertThat(disallowedHeaders("Expect,Host,Connection,Upgrade,Content-Length")).isEmpty();
  }

  @Test
  void disallowedHeadersWithWhitespaceAroundPropertyValue() {
    assertThat(disallowedHeaders(" expect,host "))
            .containsExactlyInAnyOrder("connection", "content-length", "upgrade");
  }

  @Test
  void disallowedHeadersWithWhitespaceAroundIndividualHeaders() {
    assertThat(disallowedHeaders("expect, host"))
            .containsExactlyInAnyOrder("connection", "content-length", "host", "upgrade");
    assertThat(disallowedHeaders("expect ,host"))
            .containsExactlyInAnyOrder("connection", "content-length", "expect", "upgrade");
  }

  private static Set<String> disallowedHeaders(@org.jspecify.annotations.Nullable String value) {
    String key = "jdk.httpclient.allowRestrictedHeaders";
    String original = System.getProperty(key);
    try {
      if (value == null) {
        System.clearProperty(key);
      }
      else {
        System.setProperty(key, value);
      }
      return JdkClientHttpRequest.disallowedHeaders();
    }
    finally {
      if (original == null) {
        System.clearProperty(key);
      }
      else {
        System.setProperty(key, original);
      }
    }
  }
}
