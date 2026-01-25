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

package infra.http.reactive.client;

import infra.core.io.buffer.DataBuffer;
import infra.http.DecoratingHttpMessage;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * Wraps another {@link ClientHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DecoratingClientHttpResponse extends DecoratingHttpMessage implements ClientHttpResponse {

  private final ClientHttpResponse delegate;

  public DecoratingClientHttpResponse(ClientHttpResponse delegate) {
    super(delegate);
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public ClientHttpResponse delegate() {
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
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.delegate.getCookies();
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.delegate.getBody();
  }

}
