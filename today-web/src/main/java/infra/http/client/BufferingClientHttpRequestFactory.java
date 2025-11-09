/*
 * Copyright 2017 - 2025 the original author or authors.
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
