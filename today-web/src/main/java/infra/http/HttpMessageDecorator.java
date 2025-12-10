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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Abstract base class for {@link HttpMessage} decorators.
 *
 * <p>Provides a convenient base for wrapping {@link HttpMessage} instances,
 * delegating all method calls to the wrapped instance by default.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/10 21:58
 */
public class HttpMessageDecorator implements HttpMessage {

  private final HttpMessage delegate;

  protected HttpMessageDecorator(HttpMessage delegate) {
    this.delegate = delegate;
  }

  public HttpMessage delegate() {
    return delegate;
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public @Nullable String getHeader(String name) {
    return delegate.getHeader(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return delegate.getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return delegate.getHeaderNames();
  }

  @Override
  public @Nullable MediaType getContentType() {
    return delegate.getContentType();
  }

  @Override
  public @Nullable String getContentTypeAsString() {
    return delegate.getContentTypeAsString();
  }

  @Override
  public long getContentLength() {
    return delegate.getContentLength();
  }

}
