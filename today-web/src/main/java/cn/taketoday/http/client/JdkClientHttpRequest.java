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

package cn.taketoday.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

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
    try {
      HttpRequest request = buildRequest(headers, body);
      HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      return new JdkClientHttpResponse(response);
    }
    catch (UncheckedIOException ex) {
      throw ex.getCause();
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Could not send request: " + ex.getMessage(), ex);
    }
  }

  private HttpRequest buildRequest(HttpHeaders headers, @Nullable Body body) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(this.uri);

    if (this.timeout != null) {
      builder.timeout(this.timeout);
    }

    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
        for (String headerValue : entry.getValue()) {
          builder.header(headerName, headerValue);
        }
      }
    }

    builder.method(this.method.name(), bodyPublisher(headers, body));
    return builder.build();
  }

  private HttpRequest.BodyPublisher bodyPublisher(HttpHeaders headers, @Nullable Body body) {
    if (body != null) {
      Flow.Publisher<ByteBuffer> outputStreamPublisher = OutputStreamPublisher.create(
              outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)),
              BYTE_MAPPER,
              this.executor);

      long contentLength = headers.getContentLength();
      if (contentLength != -1) {
        return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher, contentLength);
      }
      else {
        return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher);
      }
    }
    else {
      return HttpRequest.BodyPublishers.noBody();
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

}
