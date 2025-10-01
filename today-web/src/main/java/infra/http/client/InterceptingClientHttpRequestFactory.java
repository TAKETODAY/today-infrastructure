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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import infra.http.HttpMethod;
import infra.http.HttpRequest;

/**
 * {@link ClientHttpRequestFactory} wrapper with support for
 * {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestFactory
 * @see ClientHttpRequestInterceptor
 * @since 4.0
 */
public class InterceptingClientHttpRequestFactory extends ClientHttpRequestFactoryWrapper {

  private final List<ClientHttpRequestInterceptor> interceptors;

  private final Predicate<HttpRequest> bufferingPredicate;

  /**
   * Create a new instance of the {@code InterceptingClientHttpRequestFactory} with the given parameters.
   *
   * @param requestFactory the request factory to wrap
   * @param interceptors the interceptors that are to be applied (can be {@code null})
   */
  public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
          @Nullable List<ClientHttpRequestInterceptor> interceptors) {

    this(requestFactory, interceptors, null);
  }

  /**
   * Constructor variant with an additional predicate to decide whether to
   * buffer the response.
   *
   * @since 5.0
   */
  public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
          @Nullable List<ClientHttpRequestInterceptor> interceptors,
          @Nullable Predicate<HttpRequest> bufferingPredicate) {

    super(requestFactory);
    this.interceptors = interceptors != null ? interceptors : Collections.emptyList();
    this.bufferingPredicate = bufferingPredicate != null ? bufferingPredicate : req -> false;
  }

  @Override
  protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
    return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod, bufferingPredicate);
  }

}
