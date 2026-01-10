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
import java.util.List;
import java.util.function.Predicate;

import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Assert;

/**
 * Used by {@link DefaultRestClient} and {@link DefaultRestClientBuilder}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class StatusHandler {

  private StatusHandler() {
  }

  public static ResponseErrorHandler of(Predicate<HttpStatusCode> predicate, RestClient.ErrorHandler errorHandler) {
    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");
    return new FuncErrorHandler(predicate, errorHandler);
  }

  public static ResponseErrorHandler createDefaultStatusHandler(List<HttpMessageConverter<?>> messageConverters) {
    DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();
    handler.setMessageConverters(messageConverters);
    return handler;
  }

  static final class FuncErrorHandler implements ResponseErrorHandler {

    private final Predicate<HttpStatusCode> predicate;

    private final RestClient.ErrorHandler errorHandler;

    FuncErrorHandler(Predicate<HttpStatusCode> predicate, RestClient.ErrorHandler errorHandler) {
      this.predicate = predicate;
      this.errorHandler = errorHandler;
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return predicate.test(response.getStatusCode());
    }

    @Override
    public void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
      errorHandler.handle(request, response);
    }

  }

}
