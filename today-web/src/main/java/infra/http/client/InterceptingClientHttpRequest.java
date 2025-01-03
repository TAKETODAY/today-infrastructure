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
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.StreamingHttpOutputMessage;

/**
 * Wrapper for a {@link ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptor
 * ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

  private final URI uri;

  private final HttpMethod method;

  private final ClientHttpRequestFactory requestFactory;

  private final List<ClientHttpRequestInterceptor> interceptors;

  InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
          List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {

    this.requestFactory = requestFactory;
    this.interceptors = interceptors;
    this.method = method;
    this.uri = uri;
  }

  @Override
  public HttpMethod getMethod() {
    return this.method;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
    InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();
    return requestExecution.execute(this, bufferedOutput);
  }

  private class InterceptingRequestExecution implements ClientHttpRequestExecution {

    private final Iterator<ClientHttpRequestInterceptor> iterator;

    public InterceptingRequestExecution() {
      this.iterator = interceptors.iterator();
    }

    @Override
    public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
      if (iterator.hasNext()) {
        var nextInterceptor = iterator.next();
        return nextInterceptor.intercept(request, body, this);
      }

      ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), request.getMethod());
      delegate.getHeaders().addAll(request.getHeaders());

      if (request.hasAttributes()) {
        delegate.copyFrom(request);
      }

      if (body.length > 0) {
        long contentLength = delegate.getHeaders().getContentLength();
        if (contentLength > -1 && contentLength != body.length) {
          delegate.getHeaders().setContentLength(body.length);
        }
        StreamingHttpOutputMessage.writeBody(delegate, body);
      }
      return delegate.execute();
    }
  }

}
