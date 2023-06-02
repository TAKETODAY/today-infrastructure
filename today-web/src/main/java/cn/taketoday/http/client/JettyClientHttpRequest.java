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

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.OutputStreamRequestContent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
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

  private final Request request;

  private final Duration timeOut;
  private final HttpMethod method;

  public JettyClientHttpRequest(Request request, Duration timeOut) {
    this.request = request;
    this.timeOut = timeOut;
    this.method = HttpMethod.valueOf(request.getMethod());
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public String getMethodValue() {
    return method.name();
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
        try (OutputStream outputStream = requestContent.getOutputStream()) {
          body.writeTo(StreamUtils.nonClosing(outputStream));
        }
      }
      else {
        this.request.send(responseListener);
      }
      Response response = responseListener.get(TimeUnit.MILLISECONDS.convert(this.timeOut), TimeUnit.MILLISECONDS);
      return new JettyClientHttpResponse(response, responseListener.getInputStream());
    }
    catch (InterruptedException | TimeoutException | ExecutionException ex) {
      throw new IOException("Could not send request: " + ex.getMessage(), ex);
    }
  }
}
