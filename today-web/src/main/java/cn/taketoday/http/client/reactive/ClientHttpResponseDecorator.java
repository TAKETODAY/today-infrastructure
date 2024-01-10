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

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * Wraps another {@link ClientHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ClientHttpResponseDecorator implements ClientHttpResponse {

  private final ClientHttpResponse delegate;

  public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  public ClientHttpResponse getDelegate() {
    return this.delegate;
  }

  // ClientHttpResponse delegation methods...

  @Override
  public String getId() {
    return this.delegate.getId();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return this.delegate.getStatusCode();
  }

  @Override
  public int getRawStatusCode() {
    return this.delegate.getRawStatusCode();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.delegate.getHeaders();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.delegate.getCookies();
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.delegate.getBody();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
  }

}
