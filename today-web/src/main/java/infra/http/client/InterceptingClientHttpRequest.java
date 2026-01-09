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
import java.net.URI;
import java.util.List;
import java.util.function.Predicate;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;

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

  private final Predicate<HttpRequest> bufferingPredicate;

  InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
          List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method, Predicate<HttpRequest> bufferingPredicate) {

    this.requestFactory = requestFactory;
    this.interceptors = interceptors;
    this.method = method;
    this.uri = uri;
    this.bufferingPredicate = bufferingPredicate;
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
    return getExecution().execute(this, bufferedOutput);
  }

  @SuppressWarnings("NullAway")
  private ClientHttpRequestExecution getExecution() {
    ClientHttpRequestExecution execution = new EndOfChainRequestExecution(this.requestFactory);
    if (interceptors.isEmpty()) {
      return execution;
    }

    ClientHttpRequestInterceptor interceptor = null;
    for (ClientHttpRequestInterceptor current : interceptors) {
      interceptor = interceptor == null ? current : interceptor.andThen(current);
    }

    return interceptor.apply(execution);
  }

  private boolean shouldBufferResponse(HttpRequest request) {
    return bufferingPredicate.test(request);
  }

  private class EndOfChainRequestExecution implements ClientHttpRequestExecution {

    private final ClientHttpRequestFactory requestFactory;

    public EndOfChainRequestExecution(ClientHttpRequestFactory requestFactory) {
      this.requestFactory = requestFactory;
    }

    @Override
    public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
      ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), request.getMethod());
      delegate.getHeaders().addAll(request.getHeaders());

      if (request.hasAttributes()) {
        delegate.copyFrom(request);
      }

      return executeWithRequest(delegate, body, shouldBufferResponse(request));
    }
  }

}
