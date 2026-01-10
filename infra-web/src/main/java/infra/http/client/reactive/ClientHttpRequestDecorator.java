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

package infra.http.client.reactive;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.core.AttributeAccessor;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpMessageDecorator;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * Wraps another {@link ClientHttpRequest} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ClientHttpRequestDecorator extends HttpMessageDecorator implements ClientHttpRequest {

  protected final ClientHttpRequest delegate;

  public ClientHttpRequestDecorator(ClientHttpRequest delegate) {
    super(delegate);
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public ClientHttpRequest delegate() {
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
  public Iterable<String> attributeNames() {
    return delegate.attributeNames();
  }

  @Override
  public void clearAttributes() {
    delegate.clearAttributes();
  }

  @Override
  public <T> T computeAttribute(String name, Function<String, @Nullable T> computeFunction) {
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

}
