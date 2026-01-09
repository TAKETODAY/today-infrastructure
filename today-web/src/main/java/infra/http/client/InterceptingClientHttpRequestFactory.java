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
