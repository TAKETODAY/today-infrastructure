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

package cn.taketoday.http.client.support;

import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Provides a convenient implementation of the {@link HttpRequest} interface
 * that can be overridden to adapt the request.
 *
 * <p>These methods default to calling through to the wrapped request object.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class HttpRequestDecorator implements HttpRequest {

  private final HttpRequest request;

  /**
   * Create a new {@code HttpRequest} wrapping the given request object.
   *
   * @param request the request object to be wrapped
   */
  public HttpRequestDecorator(HttpRequest request) {
    Assert.notNull(request, "HttpRequest must not be null");
    this.request = request;
  }

  /**
   * Return the wrapped request.
   */
  public HttpRequest getRequest() {
    return this.request;
  }

  /**
   * Return the method of the wrapped request.
   */
  @Override
  @Nullable
  public HttpMethod getMethod() {
    return this.request.getMethod();
  }

  /**
   * Return the method value of the wrapped request.
   */
  @Override
  public String getMethodValue() {
    return this.request.getMethodValue();
  }

  /**
   * Return the URI of the wrapped request.
   */
  @Override
  public URI getURI() {
    return this.request.getURI();
  }

  /**
   * Return the headers of the wrapped request.
   */
  @Override
  public HttpHeaders getHeaders() {
    return this.request.getHeaders();
  }

}
