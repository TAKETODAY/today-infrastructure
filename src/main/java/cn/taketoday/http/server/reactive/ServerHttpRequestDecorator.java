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

package cn.taketoday.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;

/**
 * Wraps another {@link ServerHttpRequest} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServerHttpRequestDecorator implements ServerHttpRequest {

  private final ServerHttpRequest delegate;

  public ServerHttpRequestDecorator(ServerHttpRequest delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  public ServerHttpRequest getDelegate() {
    return this.delegate;
  }

  // ServerHttpRequest delegation methods...

  @Override
  public String getId() {
    return getDelegate().getId();
  }

  @Override
  @Nullable
  public HttpMethod getMethod() {
    return getDelegate().getMethod();
  }

  @Override
  public String getMethodValue() {
    return getDelegate().getMethodValue();
  }

  @Override
  public URI getURI() {
    return getDelegate().getURI();
  }

  @Override
  public RequestPath getPath() {
    return getDelegate().getPath();
  }

  @Override
  public MultiValueMap<String, String> getQueryParams() {
    return getDelegate().getQueryParams();
  }

  @Override
  public HttpHeaders getHeaders() {
    return getDelegate().getHeaders();
  }

  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    return getDelegate().getCookies();
  }

  @Override
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return getDelegate().getLocalAddress();
  }

  @Override
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return getDelegate().getRemoteAddress();
  }

  @Override
  @Nullable
  public SslInfo getSslInfo() {
    return getDelegate().getSslInfo();
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return getDelegate().getBody();
  }

  /**
   * Return the native request of the underlying server API, if possible,
   * also unwrapping {@link ServerHttpRequestDecorator} if necessary.
   *
   * @param request the request to check
   * @param <T> the expected native request type
   * @throws IllegalArgumentException if the native request can't be obtained
   */
  public static <T> T getNativeRequest(ServerHttpRequest request) {
    if (request instanceof AbstractServerHttpRequest) {
      return ((AbstractServerHttpRequest) request).getNativeRequest();
    }
    else if (request instanceof ServerHttpRequestDecorator) {
      return getNativeRequest(((ServerHttpRequestDecorator) request).getDelegate());
    }
    else {
      throw new IllegalArgumentException(
              "Can't find native request in " + request.getClass().getName());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
  }

}
