/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
