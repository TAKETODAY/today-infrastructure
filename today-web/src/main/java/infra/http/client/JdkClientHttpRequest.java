/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.http.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.lang.Nullable;
import infra.util.StreamUtils;
import infra.util.StringUtils;
import infra.util.concurrent.Future;

/**
 * {@link ClientHttpRequest} implementation based the Java {@link HttpClient}.
 * Created via the {@link JdkClientHttpRequestFactory}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JdkClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private static final OutputStreamPublisher.ByteMapper<ByteBuffer> BYTE_MAPPER = new ByteBufferMapper();

  private static final Set<String> DISALLOWED_HEADERS = disallowedHeaders();

  /**
   * By default, {@link HttpRequest} does not allow {@code Connection},
   * {@code Content-Length}, {@code Expect}, {@code Host}, or {@code Upgrade}
   * headers to be set, but this can be overriden with the
   * {@code jdk.httpclient.allowRestrictedHeaders} system property.
   *
   * @see jdk.internal.net.http.common.Utils#getDisallowedHeaders()
   */
  private static Set<String> disallowedHeaders() {
    TreeSet<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    headers.addAll(Set.of("connection", "content-length", "expect", "host", "upgrade"));

    String headersToAllow = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
    if (headersToAllow != null) {
      Set<String> toAllow = StringUtils.commaDelimitedListToSet(headersToAllow);
      headers.removeAll(toAllow);
    }
    return Collections.unmodifiableSet(headers);
  }

  private final HttpClient httpClient;

  private final HttpMethod method;

  private final URI uri;

  private final Executor executor;

  @Nullable
  private final Duration timeout;

  public JdkClientHttpRequest(HttpClient httpClient, URI uri,
          HttpMethod method, Executor executor, @Nullable Duration readTimeout) {
    this.httpClient = httpClient;
    this.uri = uri;
    this.method = method;
    this.executor = executor;
    this.timeout = readTimeout;
  }

  @Override
  public HttpMethod getMethod() {
    return this.method;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
    CompletableFuture<HttpResponse<InputStream>> responseFuture = null;
    TimeoutHandler timeoutHandler = null;

    try {
      HttpRequest request = buildRequest(headers, body);
      responseFuture = this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

      if (this.timeout != null) {
        timeoutHandler = new TimeoutHandler(responseFuture, this.timeout);
        HttpResponse<InputStream> response = responseFuture.get();
        InputStream inputStream = timeoutHandler.wrapInputStream(response);
        return new JdkClientHttpResponse(response, inputStream);
      }
      else {
        HttpResponse<InputStream> response = responseFuture.get();
        return new JdkClientHttpResponse(response, response.body());
      }
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      responseFuture.cancel(true);
      throw new IOException("Request was interrupted: " + ex.getMessage(), ex);
    }
    catch (ExecutionException ex) {
      Throwable cause = ex.getCause();

      if (cause instanceof CancellationException ce) {
        if (timeoutHandler != null) {
          timeoutHandler.handleCancellationException(ce);
        }
        throw new IOException("Request cancelled", cause);
      }
      if (cause instanceof UncheckedIOException uioEx) {
        throw uioEx.getCause();
      }
      if (cause instanceof RuntimeException rtEx) {
        throw rtEx;
      }
      else if (cause instanceof IOException ioEx) {
        throw ioEx;
      }
      else {
        throw new IOException(cause.getMessage(), cause);
      }
    }
    catch (CancellationException ex) {
      if (timeoutHandler != null) {
        timeoutHandler.handleCancellationException(ex);
      }
      throw new IOException("Request cancelled", ex);
    }
  }

  @Override
  protected Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Body body, @Nullable Executor executor) {
    HttpRequest request = buildRequest(headers, body);
    var responseFuture = Future.forAdaption(httpClient.sendAsync(request, BodyHandlers.ofInputStream()), executor);
    if (timeout != null) {
      responseFuture = responseFuture.timeout(timeout);
    }
    return responseFuture.map(JdkClientHttpResponse::new);
  }

  private HttpRequest buildRequest(HttpHeaders headers, @Nullable Body body) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(this.uri);

    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase(Locale.ROOT))) {
        for (String headerValue : entry.getValue()) {
          builder.header(headerName, headerValue);
        }
      }
    }

    if (body != null) {
      builder.method(this.method.name(), bodyPublisher(headers, body));
    }
    else {
      switch (this.method.name()) {
        case "GET":
          builder.GET();
          break;
        case "DELETE":
          builder.DELETE();
          break;
        default:
          builder.method(this.method.name(), HttpRequest.BodyPublishers.noBody());
      }
    }
    return builder.build();
  }

  private HttpRequest.BodyPublisher bodyPublisher(HttpHeaders headers, Body body) {
    var publisher = new OutputStreamPublisher<>(
            os -> body.writeTo(StreamUtils.nonClosing(os)), BYTE_MAPPER, this.executor, null);

    long contentLength = headers.getContentLength();
    if (contentLength > 0) {
      return HttpRequest.BodyPublishers.fromPublisher(publisher, contentLength);
    }
    else if (contentLength == 0) {
      return HttpRequest.BodyPublishers.noBody();
    }
    else {
      return HttpRequest.BodyPublishers.fromPublisher(publisher);
    }
  }

  private static final class ByteBufferMapper implements OutputStreamPublisher.ByteMapper<ByteBuffer> {

    @Override
    public ByteBuffer map(int b) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(1);
      byteBuffer.put((byte) b);
      byteBuffer.flip();
      return byteBuffer;
    }

    @Override
    public ByteBuffer map(byte[] b, int off, int len) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(len);
      byteBuffer.put(b, off, len);
      byteBuffer.flip();
      return byteBuffer;
    }

  }

  /**
   * Temporary workaround to use instead of {@link HttpRequest.Builder#timeout(Duration)}
   * until <a href="https://bugs.openjdk.org/browse/JDK-8258397">JDK-8258397</a>
   * is fixed. Essentially, create a future wiht a timeout handler, and use it
   * to close the response.
   *
   * @see <a href="https://mail.openjdk.org/pipermail/net-dev/2021-October/016672.html">OpenJDK discussion thread</a>
   */
  private static final class TimeoutHandler {

    private final CompletableFuture<Void> timeoutFuture;

    private final AtomicBoolean timeout = new AtomicBoolean(false);

    private TimeoutHandler(CompletableFuture<HttpResponse<InputStream>> future, Duration timeout) {
      this.timeoutFuture = new CompletableFuture<Void>()
              .completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS);

      this.timeoutFuture.thenRun(() -> {
        this.timeout.set(true);
        if (future.cancel(true) || future.isCompletedExceptionally() || !future.isDone()) {
          return;
        }
        try {
          future.get().body().close();
        }
        catch (Exception ex) {
          // ignore
        }
      });
    }

    @Nullable
    public InputStream wrapInputStream(HttpResponse<InputStream> response) {
      InputStream body = response.body();
      if (body == null) {
        return null;
      }
      return new FilterInputStream(body) {

        @Override
        public void close() throws IOException {
          TimeoutHandler.this.timeoutFuture.cancel(false);
          super.close();
        }
      };
    }

    public void handleCancellationException(CancellationException ex) throws HttpTimeoutException {
      if (this.timeout.get()) {
        throw new HttpTimeoutException(ex.getMessage());
      }
    }

  }

}
