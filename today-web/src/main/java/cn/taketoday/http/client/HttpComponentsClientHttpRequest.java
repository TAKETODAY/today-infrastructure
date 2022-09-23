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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
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
  private final ClassicHttpRequest httpRequest;

  HttpComponentsClientHttpRequest(HttpClient client, ClassicHttpRequest request, HttpContext context) {
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
    try {
      return this.httpRequest.getUri();
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }

  HttpContext getHttpContext() {
    return this.httpContext;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
    addHeaders(this.httpRequest, headers);

    ContentType contentType = ContentType.parse(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput, contentType);
    this.httpRequest.setEntity(requestEntity);
    HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
    Assert.isInstanceOf(ClassicHttpResponse.class, httpResponse,
            "HttpResponse not an instance of ClassicHttpResponse");
    return new HttpComponentsClientHttpResponse((ClassicHttpResponse) httpResponse);
  }

  /**
   * Add the given headers to the given HTTP request.
   *
   * @param httpRequest the request to add the headers to
   * @param headers the headers to add
   */
  static void addHeaders(ClassicHttpRequest httpRequest, HttpHeaders headers) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
        String headerValue = StringUtils.collectionToDelimitedString(entry.getValue(), "; ");
        httpRequest.addHeader(headerName, headerValue);
      }
      else if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
              && !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
        for (String headerValue : entry.getValue()) {
          httpRequest.addHeader(headerName, headerValue);
        }
      }
    }
  }

}
