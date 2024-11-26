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
import java.io.OutputStream;
import java.util.concurrent.Executor;

import infra.http.AbstractHttpRequest;
import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.concurrent.Future;

/**
 * Abstract base for {@link ClientHttpRequest} that makes sure that headers
 * and body are not written multiple times.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractClientHttpRequest extends AbstractHttpRequest implements ClientHttpRequest {

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private boolean executed = false;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  @Override
  public final HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (this.executed) {
      this.readOnlyHeaders = headers.asReadOnly();
      return this.readOnlyHeaders;
    }
    else {
      return this.headers;
    }
  }

  @Override
  public final OutputStream getBody() throws IOException {
    assertNotExecuted();
    return getBodyInternal(this.headers);
  }

  @Override
  public final ClientHttpResponse execute() throws IOException {
    assertNotExecuted();
    ClientHttpResponse result = executeInternal(this.headers);
    this.executed = true;
    return result;
  }

  @Override
  public final Future<ClientHttpResponse> async(@Nullable Executor executor) {
    assertNotExecuted();
    this.executed = true;
    return asyncInternal(headers, executor);
  }

  /**
   * Assert that this request has not been {@linkplain #execute() executed} yet.
   *
   * @throws IllegalStateException if this request has been executed
   */
  protected void assertNotExecuted() {
    Assert.state(!this.executed, "ClientHttpRequest already executed");
  }

  /**
   * Abstract template method that returns the body.
   *
   * @param headers the HTTP headers
   * @return the body output stream
   */
  protected abstract OutputStream getBodyInternal(HttpHeaders headers)
          throws IOException;

  /**
   * Abstract template method that writes the given headers and content to the HTTP request.
   *
   * @param headers the HTTP headers
   * @return the response object for the executed request
   */
  protected abstract ClientHttpResponse executeInternal(HttpHeaders headers)
          throws IOException;

  /**
   * Abstract template method that writes the given headers and content to the HTTP request.
   *
   * @param headers the HTTP headers
   * @return the response object for the executed request
   */
  protected Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Executor executor) {
    // todo 这样实现肯定不行
    return Future.run(() -> executeInternal(headers), executor);
  }

}
