/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import org.reactivestreams.Publisher;

import java.util.function.Supplier;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * Wraps another {@link ServerHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServerHttpResponseDecorator implements ServerHttpResponse {
  private final ServerHttpResponse delegate;

  public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  public ServerHttpResponse getDelegate() {
    return this.delegate;
  }

  // ServerHttpResponse delegation methods...

  @Override
  public boolean setStatusCode(@Nullable HttpStatus status) {
    return getDelegate().setStatusCode(status);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return getDelegate().getStatusCode();
  }

  @Override
  public boolean setRawStatusCode(@Nullable Integer value) {
    return getDelegate().setRawStatusCode(value);
  }

  @Override
  public Integer getRawStatusCode() {
    return getDelegate().getRawStatusCode();
  }

  @Override
  public HttpHeaders getHeaders() {
    return getDelegate().getHeaders();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return getDelegate().getCookies();
  }

  @Override
  public void addCookie(ResponseCookie cookie) {
    getDelegate().addCookie(cookie);
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return getDelegate().bufferFactory();
  }

  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    getDelegate().beforeCommit(action);
  }

  @Override
  public boolean isCommitted() {
    return getDelegate().isCommitted();
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return getDelegate().writeWith(body);
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return getDelegate().writeAndFlushWith(body);
  }

  @Override
  public Mono<Void> setComplete() {
    return getDelegate().setComplete();
  }

  /**
   * Return the native response of the underlying server API, if possible,
   * also unwrapping {@link ServerHttpResponseDecorator} if necessary.
   *
   * @param response the response to check
   * @param <T> the expected native response type
   * @throws IllegalArgumentException if the native response can't be obtained
   */
  public static <T> T getNativeResponse(ServerHttpResponse response) {
    if (response instanceof AbstractServerHttpResponse) {
      return ((AbstractServerHttpResponse) response).getNativeResponse();
    }
    else if (response instanceof ServerHttpResponseDecorator) {
      return getNativeResponse(((ServerHttpResponseDecorator) response).getDelegate());
    }
    else {
      throw new IllegalArgumentException(
              "Can't find native response in " + response.getClass().getName());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
  }

}
