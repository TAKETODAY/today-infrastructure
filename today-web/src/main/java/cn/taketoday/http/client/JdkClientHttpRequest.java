/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

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

  /*
   * The JDK HttpRequest doesn't allow all headers to be set. The named headers are taken from the default
   * implementation for HttpRequest.
   */
  private static final List<String> DISALLOWED_HEADERS =
          List.of("connection", "content-length", "expect", "host", "upgrade");

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

    headers.forEach((headerName, headerValues) -> {
      if (!headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
        if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
          for (String headerValue : headerValues) {
            builder.header(headerName, headerValue);
          }
        }
      }
    });

    builder.method(this.method.name(), bodyPublisher(headers, body));
    return builder.build();
  }

  private HttpRequest.BodyPublisher bodyPublisher(HttpHeaders headers, @Nullable Body body) {
    if (body != null) {
      Flow.Publisher<ByteBuffer> outputStreamPublisher = OutputStreamPublisher.create(
              outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)),
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

}
