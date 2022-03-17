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

package cn.taketoday.http.server;

import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;

/**
 * Implementation of {@code ServerHttpResponse} that delegates all calls to a
 * given target {@code ServerHttpResponse}.
 *
 * @author Arjen Poutsma
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
    Assert.notNull(delegate, "Delegate must not be null");
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
  public void setStatusCode(HttpStatus status) {
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
