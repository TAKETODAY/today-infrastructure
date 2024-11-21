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
import java.util.function.Predicate;

import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.web.client.RestClient.ErrorHandler;

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
