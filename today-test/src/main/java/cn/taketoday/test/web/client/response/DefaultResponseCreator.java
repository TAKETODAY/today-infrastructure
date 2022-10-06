/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.client.response;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.http.client.MockClientHttpResponse;
import cn.taketoday.test.web.client.ResponseCreator;

/**
 * A {@code ResponseCreator} with builder-style methods for adding response details.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultResponseCreator implements ResponseCreator {

  private final HttpStatusCode statusCode;

  private byte[] content = new byte[0];

  @Nullable
  private Resource contentResource;

  private final HttpHeaders headers = HttpHeaders.create();

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
    Assert.notNull(statusCode, "HttpStatusCode must not be null");
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
