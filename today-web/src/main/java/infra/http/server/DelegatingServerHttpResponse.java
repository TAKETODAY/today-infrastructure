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

package infra.http.server;

import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.lang.Assert;

/**
 * Implementation of {@code ServerHttpResponse} that delegates all calls to a
 * given target {@code ServerHttpResponse}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class DelegatingServerHttpResponse implements ServerHttpResponse {

  private final ServerHttpResponse delegate;

  /**
   * Create a new {@code DelegatingServerHttpResponse}.
   *
   * @param delegate the response to delegate to
   */
  public DelegatingServerHttpResponse(ServerHttpResponse delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  /**
   * Returns the target response that this response delegates to.
   *
   * @return the delegate
   */
  public ServerHttpResponse getDelegate() {
    return this.delegate;
  }

  @Override
  public void setStatusCode(HttpStatusCode status) {
    this.delegate.setStatusCode(status);
  }

  @Override
  public void flush() throws IOException {
    this.delegate.flush();
  }

  @Override
  public void close() {
    this.delegate.close();
  }

  @Override
  public OutputStream getBody() throws IOException {
    return this.delegate.getBody();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.delegate.getHeaders();
  }

}
