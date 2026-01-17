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

package infra.http.client.support;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import infra.core.AttributeAccessor;
import infra.http.HttpMessageDecorator;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.lang.Assert;

/**
 * Provides a convenient implementation of the {@link HttpRequest} interface
 * that can be overridden to adapt the request.
 *
 * <p>These methods default to calling through to the wrapped request object.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpRequestDecorator extends HttpMessageDecorator implements HttpRequest {

  protected final HttpRequest delegate;

  /**
   * Create a new {@code HttpRequest} wrapping the given request object.
   *
   * @param delegate the request object to be wrapped
   */
  public HttpRequestDecorator(HttpRequest delegate) {
    super(delegate);
    Assert.notNull(delegate, "delegate is required");
    this.delegate = delegate;
  }

  /**
   * Return the wrapped request.
   */
  @Override
  public HttpRequest delegate() {
    return this.delegate;
  }

  /**
   * Return the method of the wrapped request.
   */
  @Override
  public HttpMethod getMethod() {
    return this.delegate.getMethod();
  }

  /**
   * Return the method value of the wrapped request.
   */
  @Override
  public String getMethodAsString() {
    return this.delegate.getMethodAsString();
  }

  /**
   * Return the URI of the wrapped request.
   */
  @Override
  public URI getURI() {
    return this.delegate.getURI();
  }

  // AttributeAccessor

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

}
