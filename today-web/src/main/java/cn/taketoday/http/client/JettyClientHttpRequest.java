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

import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.OutputStreamRequestContent;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation based on Jetty's
 * {@link org.eclipse.jetty.client.HttpClient}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JettyClientHttpRequestFactory
 * @since 4.0
 */
class JettyClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private static final int CHUNK_SIZE = 1024;

  private final Request request;

  private final long readTimeout;

  public JettyClientHttpRequest(Request request, long readTimeout) {
    this.request = request;
    this.readTimeout = readTimeout;
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(this.request.getMethod());
  }

  @Override
  public URI getURI() {
    return this.request.getURI();
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
    if (!headers.isEmpty()) {
      this.request.headers(httpFields -> {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
          String headerName = entry.getKey();
          List<String> headerValues = entry.getValue();
          for (String headerValue : headerValues) {
            httpFields.add(headerName, headerValue);
          }
        }
      });
    }
    String contentType = null;
    if (headers.getContentType() != null) {
      contentType = headers.getContentType().toString();
    }
    try {
      InputStreamResponseListener responseListener = new InputStreamResponseListener();
      if (body != null) {
        OutputStreamRequestContent requestContent = new OutputStreamRequestContent(contentType);
        this.request.body(requestContent)
                .send(responseListener);
        try (var outputStream = new BufferedOutputStream(requestContent.getOutputStream(), CHUNK_SIZE)) {
          body.writeTo(StreamUtils.nonClosing(outputStream));
        }
      }
      else {
        this.request.send(responseListener);
      }
      Response response = responseListener.get(this.readTimeout, TimeUnit.MILLISECONDS);
      return new JettyClientHttpResponse(response, responseListener.getInputStream());
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Request was interrupted: " + ex.getMessage(), ex);
    }
    catch (ExecutionException ex) {
      Throwable cause = ex.getCause();

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
    catch (TimeoutException ex) {
      throw new IOException("Request timed out: " + ex.getMessage(), ex);
    }
  }
}
