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

package infra.web.client;

import java.io.IOException;
import java.util.function.Predicate;

import infra.http.HttpRequest;
import infra.http.client.ClientHttpResponse;
import infra.web.client.RestClient.ErrorHandler;

/**
 * A basic, no operation {@link ResponseErrorHandler} implementation suitable
 * for ignoring any error using the {@link RestTemplate}.
 * <p>This implementation is not suitable with the {@link RestClient} as it uses
 * a list of candidates where the first matching is invoked. If you want to
 * disable default status handlers with the {@code RestClient}, consider
 * registering a noop {@link ErrorHandler ErrorHandler} with a
 * predicate that matches all status code, see
 * {@link RestClient.Builder#defaultStatusHandler(Predicate, ErrorHandler)}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class NoOpResponseErrorHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    return false;
  }

  @Override
  public void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {

  }

}
