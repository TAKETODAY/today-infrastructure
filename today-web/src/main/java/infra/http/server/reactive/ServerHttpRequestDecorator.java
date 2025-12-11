/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.server.reactive;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.core.io.buffer.DataBuffer;
import infra.http.HttpCookie;
import infra.http.HttpMessageDecorator;
import infra.http.HttpMethod;
import infra.http.server.RequestPath;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * Wraps another {@link ServerHttpRequest} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServerHttpRequestDecorator extends HttpMessageDecorator implements ServerHttpRequest {

  private final ServerHttpRequest delegate;

  public ServerHttpRequestDecorator(ServerHttpRequest delegate) {
    super(delegate);
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public ServerHttpRequest delegate() {
    return this.delegate;
  }

  // ServerHttpRequest delegation methods...

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public HttpMethod getMethod() {
    return delegate.getMethod();
  }

  @Override
  public String getMethodAsString() {
    return delegate.getMethodAsString();
  }

  @Override
  public URI getURI() {
    return delegate.getURI();
  }

  @Override
  public RequestPath getPath() {
    return delegate.getPath();
  }

  @Override
  public MultiValueMap<String, String> getQueryParams() {
    return delegate.getQueryParams();
  }

  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    return delegate.getCookies();
  }

  @Override
  @Nullable
  public InetSocketAddress getLocalAddress() {
    return delegate.getLocalAddress();
  }

  @Override
  @Nullable
  public InetSocketAddress getRemoteAddress() {
    return delegate.getRemoteAddress();
  }

  @Override
  @Nullable
  public SslInfo getSslInfo() {
    return delegate.getSslInfo();
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return delegate.getBody();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
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
      return getNativeRequest(((ServerHttpRequestDecorator) request).delegate());
    }
    else {
      throw new IllegalArgumentException(
              "Can't find native request in " + request.getClass().getName());
    }
  }

}
