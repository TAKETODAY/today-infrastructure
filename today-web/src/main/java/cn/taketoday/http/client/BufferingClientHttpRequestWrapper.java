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

package cn.taketoday.http.client;

import java.io.IOException;
import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.StreamingHttpOutputMessage;

/**
 * Simple implementation of {@link ClientHttpRequest} that wraps another request.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BufferingClientHttpRequestWrapper extends AbstractBufferingClientHttpRequest {

  private final ClientHttpRequest request;

  BufferingClientHttpRequestWrapper(ClientHttpRequest request) {
    this.request = request;
  }

  @Override
  public HttpMethod getMethod() {
    return this.request.getMethod();
  }

  @Override
  public String getMethodValue() {
    return this.request.getMethodValue();
  }

  @Override
  public URI getURI() {
    return this.request.getURI();
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
    request.getHeaders().putAll(headers);
    StreamingHttpOutputMessage.writeBody(request, bufferedOutput);

    ClientHttpResponse response = request.execute();
    return new BufferingClientHttpResponseWrapper(response);
  }

}
