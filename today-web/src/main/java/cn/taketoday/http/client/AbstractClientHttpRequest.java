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

package cn.taketoday.http.client;

import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base for {@link ClientHttpRequest} that makes sure that headers
 * and body are not written multiple times.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

  private final HttpHeaders headers = HttpHeaders.create();

  private boolean executed = false;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  @Override
  public final HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (this.executed) {
      this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
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
  protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

  /**
   * Abstract template method that writes the given headers and content to the HTTP request.
   *
   * @param headers the HTTP headers
   * @return the response object for the executed request
   */
  protected abstract ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException;

}
