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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation that uses standard JDK facilities to
 * execute buffered requests. Created via the {@link SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see SimpleClientHttpRequestFactory#createRequest(URI, HttpMethod)
 * @since 4.0
 */
final class SimpleClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private final HttpURLConnection connection;

  private final int chunkSize;

  SimpleClientHttpRequest(HttpURLConnection connection, int chunkSize) {
    this.connection = connection;
    this.chunkSize = chunkSize;
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(this.connection.getRequestMethod());
  }

  @Override
  public String getMethodValue() {
    return connection.getRequestMethod();
  }

  @Override
  public URI getURI() {
    try {
      return this.connection.getURL().toURI();
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
    }
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
    if (connection.getDoOutput()) {
      long contentLength = headers.getContentLength();
      if (contentLength >= 0) {
        connection.setFixedLengthStreamingMode(contentLength);
      }
      else {
        connection.setChunkedStreamingMode(chunkSize);
      }
    }
    addHeaders(connection, headers);
    connection.connect();

    if (connection.getDoOutput() && body != null) {
      try (OutputStream os = connection.getOutputStream()) {
        body.writeTo(os);
      }
    }
    else {
      // Immediately trigger the request in a no-output scenario as well
      connection.getResponseCode();
    }
    return new SimpleClientHttpResponse(connection);
  }

  /**
   * Add the given headers to the given HTTP connection.
   *
   * @param connection the connection to add the headers to
   * @param headers the headers to add
   */
  static void addHeaders(HttpURLConnection connection, HttpHeaders headers) {
    String method = connection.getRequestMethod();
    if ((method.equals("PUT") || method.equals("DELETE"))
            && StringUtils.isBlank(headers.getFirst(HttpHeaders.ACCEPT))) {
      // Avoid "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
      // from HttpUrlConnection which prevents JSON error response details.
      headers.set(HttpHeaders.ACCEPT, "*/*");
    }

    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      List<String> headerValues = entry.getValue();
      if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
        String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
        connection.setRequestProperty(headerName, headerValue);
      }
      else {
        for (String headerValue : headerValues) {
          String actualHeaderValue = headerValue != null ? headerValue : "";
          connection.addRequestProperty(headerName, actualHeaderValue);
        }
      }
    }
  }

}
