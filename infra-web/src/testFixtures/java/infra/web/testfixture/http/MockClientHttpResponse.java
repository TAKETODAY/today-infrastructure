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

package infra.web.testfixture.http;

import java.io.IOException;
import java.io.InputStream;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MockHttpInputMessage;
import infra.lang.Assert;
import org.jspecify.annotations.Nullable;
import infra.web.client.ClientResponse;

/**
 * Mock implementation of {@link infra.http.client.ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientResponse {

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
    Assert.notNull(statusCode, "HttpStatusCode is required");
    this.statusCode = statusCode;
  }

  /**
   * Create a {@code MockClientHttpResponse} with response body as a byte array
   * and a custom HTTP status code.
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
    Assert.notNull(statusCode, "HttpStatusCode is required");
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

  @Nullable
  @Override
  public <T> T bodyTo(Class<T> bodyType) {
    return null;
  }

  @Nullable
  @Override
  public <T> T bodyTo(ParameterizedTypeReference<T> bodyType) {
    return null;
  }

}
