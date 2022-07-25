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

package cn.taketoday.http.client.reactive;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.function.Supplier;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;

/**
 * Wraps another {@link ClientHttpRequest} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ClientHttpRequestDecorator implements ClientHttpRequest {

  private final ClientHttpRequest delegate;

  public ClientHttpRequestDecorator(ClientHttpRequest delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  public ClientHttpRequest getDelegate() {
    return this.delegate;
  }

  // ClientHttpRequest delegation methods...

  @Override
  public HttpMethod getMethod() {
    return this.delegate.getMethod();
  }

  @Override
  public URI getURI() {
    return this.delegate.getURI();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.delegate.getHeaders();
  }

  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    return this.delegate.getCookies();
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return this.delegate.bufferFactory();
  }

  @Override
  public <T> T getNativeRequest() {
    return this.delegate.getNativeRequest();
  }

  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    this.delegate.beforeCommit(action);
  }

  @Override
  public boolean isCommitted() {
    return this.delegate.isCommitted();
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return this.delegate.writeWith(body);
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return this.delegate.writeAndFlushWith(body);
  }

  @Override
  public Mono<Void> setComplete() {
    return this.delegate.setComplete();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
  }

}
