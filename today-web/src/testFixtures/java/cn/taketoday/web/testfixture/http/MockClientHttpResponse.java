/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.testfixture.http;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;

/**
 * Mock implementation of {@link cn.taketoday.http.client.ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {

  private final HttpStatusCode statusCode;

  /**
   * Create a {@code MockClientHttpResponse} with an empty response body and
   * HTTP status code {@link HttpStatus#OK OK}.
   */
  public MockClientHttpResponse() {
    this(new byte[0], HttpStatus.OK);
  }

  /**
   * Create a {@code MockClientHttpResponse} with response body as a byte array
   * and the supplied HTTP status code.
   */
  public MockClientHttpResponse(byte[] body, HttpStatusCode statusCode) {
    super(body);
    Assert.notNull(statusCode, "HttpStatusCode must not be null");
    this.statusCode = statusCode;
  }

  /**
   * Create a {@code MockClientHttpResponse} with response body as a byte array
   * and a custom HTTP status code.
   *
   * @since 5.3.17
   */
  public MockClientHttpResponse(byte[] body, int statusCode) {
    this(body, HttpStatusCode.valueOf(statusCode));
  }

  /**
   * Create a {@code MockClientHttpResponse} with response body as {@link InputStream}
   * and the supplied HTTP status code.
   */
  public MockClientHttpResponse(InputStream body, HttpStatusCode statusCode) {
    super(body);
    Assert.notNull(statusCode, "HttpStatusCode must not be null");
    this.statusCode = statusCode;
  }

  /**
   * Create a {@code MockClientHttpResponse} with response body as {@link InputStream}
   * and a custom HTTP status code.
   */
  public MockClientHttpResponse(InputStream body, int statusCode) {
    this(body, HttpStatusCode.valueOf(statusCode));
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  @Override
  public String getStatusText() {
    return (this.statusCode instanceof HttpStatus status ? status.getReasonPhrase() : "");
  }

  @Override
  public void close() {
    try {
      getBody().close();
    }
    catch (IOException ex) {
      // ignore
    }
  }

}
