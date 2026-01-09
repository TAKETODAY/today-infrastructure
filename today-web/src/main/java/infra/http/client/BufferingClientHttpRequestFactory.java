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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.function.Predicate;

import infra.http.HttpMethod;
import infra.http.HttpRequest;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that buffers
 * all outgoing and incoming streams in memory.
 *
 * <p>Using this wrapper allows for multiple reads of the
 * {@linkplain ClientHttpResponse#getBody() response body}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BufferingClientHttpRequestFactory extends ClientHttpRequestFactoryWrapper {

  private final Predicate<HttpRequest> bufferingPredicate;

  /**
   * Create a buffering wrapper for the given {@link ClientHttpRequestFactory}.
   *
   * @param requestFactory the target request factory to wrap
   */
  public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
    this(requestFactory, null);
  }

  /**
   * Constructor variant with an additional predicate to decide whether to
   * buffer the response.
   *
   * @since 5.0
   */
  public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
          @Nullable Predicate<HttpRequest> bufferingPredicate) {
    super(requestFactory);
    this.bufferingPredicate = bufferingPredicate != null ? bufferingPredicate : request -> true;
  }

  @Override
  protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException {
    ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
    if (shouldBuffer(request)) {
      return new BufferingClientHttpRequestWrapper(request);
    }
    else {
      return request;
    }
  }

  /**
   * Indicates whether the request/response exchange for the given URI and method
   * should be buffered in memory.
   * <p>The default implementation returns {@code true} for all URIs and methods.
   * Subclasses can override this method to change this behavior.
   *
   * @param request the request
   * @return {@code true} if the exchange should be buffered; {@code false} otherwise
   */
  protected boolean shouldBuffer(HttpRequest request) {
    return this.bufferingPredicate.test(request);
  }

}
