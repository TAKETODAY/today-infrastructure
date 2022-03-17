/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 * @since 4.0
 */
final class HttpComponentsClientHttpRequest extends AbstractBufferingClientHttpRequest {

  private final HttpClient httpClient;
  private final HttpContext httpContext;
  private final HttpUriRequest httpRequest;

  HttpComponentsClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
    this.httpClient = client;
    this.httpRequest = request;
    this.httpContext = context;
  }

  @Override
  public String getMethodValue() {
    return this.httpRequest.getMethod();
  }

  @Override
  public URI getURI() {
    return this.httpRequest.getURI();
  }

  HttpContext getHttpContext() {
    return this.httpContext;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
    addHeaders(this.httpRequest, headers);

    if (this.httpRequest instanceof HttpEntityEnclosingRequest entityEnclosingRequest) {
      HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
      entityEnclosingRequest.setEntity(requestEntity);
    }
    HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
    return new HttpComponentsClientHttpResponse(httpResponse);
  }

  /**
   * Add the given headers to the given HTTP request.
   *
   * @param httpRequest the request to add the headers to
   * @param headers the headers to add
   */
  static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
        String headerValue = StringUtils.collectionToDelimitedString(entry.getValue(), "; ");
        httpRequest.addHeader(headerName, headerValue);
      }
      else if (!HTTP.CONTENT_LEN.equalsIgnoreCase(headerName)
              && !HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
        for (String headerValue : entry.getValue()) {
          httpRequest.addHeader(headerName, headerValue);
        }
      }
    }
  }

}
