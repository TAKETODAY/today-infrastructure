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

package infra.http.client;

import java.io.IOException;
import java.io.InputStream;

import infra.http.DecoratingHttpMessage;
import infra.http.HttpStatusCode;
import infra.lang.Assert;

/**
 * Decorator for {@link ClientHttpResponse} that allows for wrapping and extending
 * the functionality of an existing response implementation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/9 14:57
 */
public class DecoratingClientHttpResponse extends DecoratingHttpMessage implements ClientHttpResponse {

  protected final ClientHttpResponse delegate;

  public DecoratingClientHttpResponse(ClientHttpResponse delegate) {
    super(delegate);
    Assert.notNull(delegate, "ClientHttpResponse delegate is required");
    this.delegate = delegate;
  }

  @Override
  public InputStream getBody() throws IOException {
    return delegate.getBody();
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

  @Override
  public ClientHttpResponse delegate() {
    return delegate;
  }

}
