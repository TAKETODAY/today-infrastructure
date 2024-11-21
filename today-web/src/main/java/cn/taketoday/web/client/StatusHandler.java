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

package cn.taketoday.web.client;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;

/**
 * Used by {@link DefaultRestClient} and {@link DefaultRestClientBuilder}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class StatusHandler {

  public static ResponseErrorHandler of(Predicate<HttpStatusCode> predicate, RestClient.ErrorHandler errorHandler) {
    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");
    return new FuncErrorHandler(predicate, errorHandler);
  }

  public static ResponseErrorHandler defaultHandler(List<HttpMessageConverter<?>> messageConverters) {
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
