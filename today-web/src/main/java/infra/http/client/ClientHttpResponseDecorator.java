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

package infra.http.client;

import java.io.IOException;
import java.io.InputStream;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.lang.Assert;

/**
 * ClientHttpResponse Decorator
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/9 14:57
 */
public class ClientHttpResponseDecorator implements ClientHttpResponse {
  protected final ClientHttpResponse delegate;

  public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
    Assert.notNull(delegate, "ClientHttpResponse delegate is required");
    this.delegate = delegate;
  }

  @Override
  public InputStream getBody() throws IOException {
    return delegate.getBody();
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return delegate.getStatusCode();
  }

  @Override
  public int getRawStatusCode() {
    return delegate.getRawStatusCode();
  }

  @Override
  public String getStatusText() {
    return delegate.getStatusText();
  }

  @Override
  public void close() {
    delegate.close();
  }

  public ClientHttpResponse getDelegate() {
    return delegate;
  }

}
