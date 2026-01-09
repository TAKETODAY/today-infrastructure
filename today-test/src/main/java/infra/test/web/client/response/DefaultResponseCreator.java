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

package infra.test.web.client.response;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.mock.http.client.MockClientHttpResponse;
import infra.test.web.client.ResponseCreator;
import infra.util.MultiValueMap;

/**
 * A {@code ResponseCreator} with builder-style methods for adding response details.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultResponseCreator implements ResponseCreator {

  private final HttpStatusCode statusCode;

  private byte[] content = new byte[0];

  @Nullable
  private Resource contentResource;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  /**
   * Protected constructor.
   * Use static factory methods in {@link MockRestResponseCreators}.
   */
  protected DefaultResponseCreator(int statusCode) {
    this(HttpStatusCode.valueOf(statusCode));
  }

  /**
   * Protected constructor.
   * Use static factory methods in {@link MockRestResponseCreators}.
   */
  protected DefaultResponseCreator(HttpStatusCode statusCode) {
    Assert.notNull(statusCode, "HttpStatusCode is required");
    this.statusCode = statusCode;
  }

  /**
   * Set the body as a UTF-8 String.
   */
  public DefaultResponseCreator body(String content) {
    this.content = content.getBytes(StandardCharsets.UTF_8);
    return this;
  }

  /**
   * Set the body from a string using the given character set.
   */
  public DefaultResponseCreator body(String content, Charset charset) {
    this.content = content.getBytes(charset);
    return this;
  }

  /**
   * Set the body as a byte array.
   */
  public DefaultResponseCreator body(byte[] content) {
    this.content = content;
    return this;
  }

  /**
   * Set the body from a {@link Resource}.
   */
  public DefaultResponseCreator body(Resource resource) {
    this.contentResource = resource;
    return this;
  }

  /**
   * Set the {@code Content-Type} header.
   */
  public DefaultResponseCreator contentType(MediaType mediaType) {
    this.headers.setContentType(mediaType);
    return this;
  }

  /**
   * Set the {@code Location} header.
   */
  public DefaultResponseCreator location(URI location) {
    this.headers.setLocation(location);
    return this;
  }

  /**
   * Add a response header with one or more values.
   */
  public DefaultResponseCreator header(String name, String... headerValues) {
    for (String headerValue : headerValues) {
      this.headers.add(name, headerValue);
    }
    return this;
  }

  /**
   * Copy all given headers.
   */
  public DefaultResponseCreator headers(HttpHeaders headers) {
    this.headers.putAll(headers);
    return this;
  }

  /**
   * Add one or more cookies.
   */
  public DefaultResponseCreator cookies(ResponseCookie... cookies) {
    for (ResponseCookie cookie : cookies) {
      this.headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    return this;
  }

  /**
   * Copy all cookies from the given {@link MultiValueMap}.
   */
  public DefaultResponseCreator cookies(MultiValueMap<String, ResponseCookie> multiValueMap) {
    multiValueMap.values().forEach(cookies -> cookies.forEach(this::cookies));
    return this;
  }

  @Override
  public ClientHttpResponse createResponse(@Nullable ClientHttpRequest request) throws IOException {
    MockClientHttpResponse response = (this.contentResource != null ?
                                       new MockClientHttpResponse(this.contentResource.getInputStream(), this.statusCode) :
                                       new MockClientHttpResponse(this.content, this.statusCode));
    response.getHeaders().putAll(this.headers);
    return response;
  }

}
