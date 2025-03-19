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

package infra.mock.http.client;

import java.io.IOException;
import java.io.InputStream;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.mock.http.MockHttpInputMessage;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {

  private final HttpStatusCode statusCode;

  /**
   * Constructor with response body as a byte array.
   */
  public MockClientHttpResponse(byte[] body, HttpStatusCode statusCode) {
    super(body);
    Assert.notNull(statusCode, "HttpStatusCode is required");
    this.statusCode = statusCode;
  }

  /**
   * Variant of {@link #MockClientHttpResponse(byte[], HttpStatusCode)} with a
   * custom HTTP status code.
   */
  public MockClientHttpResponse(byte[] body, int statusCode) {
    super(body);
    this.statusCode = HttpStatusCode.valueOf(statusCode);
  }

  /**
   * Constructor with response body as InputStream.
   */
  public MockClientHttpResponse(InputStream body, HttpStatusCode statusCode) {
    super(body);
    Assert.notNull(statusCode, "HttpStatus is required");
    this.statusCode = statusCode;
  }

  /**
   * Variant of {@link #MockClientHttpResponse(InputStream, HttpStatusCode)} with a
   * custom HTTP status code.
   */
  public MockClientHttpResponse(InputStream body, int statusCode) {
    super(body);
    this.statusCode = HttpStatusCode.valueOf(statusCode);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return statusCode;
  }

  @Override
  public int getRawStatusCode() {
    return this.statusCode.value();
  }

  @Override
  public String getStatusText() {
    if (this.statusCode instanceof HttpStatus status) {
      return status.getReasonPhrase();
    }
    else {
      return "";
    }
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
