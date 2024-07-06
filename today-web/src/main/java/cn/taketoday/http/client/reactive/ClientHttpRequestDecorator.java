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

package cn.taketoday.http.client.reactive;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * Wraps another {@link ClientHttpRequest} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  public void setAttributes(@Nullable Map<String, Object> attributes) {
    delegate.setAttributes(attributes);
  }

  @Override
  public Iterator<String> attributeNames() {
    return delegate.attributeNames();
  }

  @Override
  public void clearAttributes() {
    delegate.clearAttributes();
  }

  @Override
  public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
    return delegate.computeAttribute(name, computeFunction);
  }

  @Override
  public void copyFrom(AttributeAccessor source) {
    delegate.copyFrom(source);
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public String[] getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public boolean hasAttribute(String name) {
    return delegate.hasAttribute(name);
  }

  @Override
  public boolean hasAttributes() {
    return delegate.hasAttributes();
  }

  @Override
  @Nullable
  public Object removeAttribute(String name) {
    return delegate.removeAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    delegate.setAttribute(name, value);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
  }

}
