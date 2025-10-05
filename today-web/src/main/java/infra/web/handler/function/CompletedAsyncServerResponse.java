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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import infra.web.RequestContext;

/**
 * {@link AsyncServerResponse} implementation for completed futures.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class CompletedAsyncServerResponse implements AsyncServerResponse {

  private final ServerResponse serverResponse;

  CompletedAsyncServerResponse(ServerResponse serverResponse) {
    Assert.notNull(serverResponse, "ServerResponse is required");
    this.serverResponse = serverResponse;
  }

  @Override
  public ServerResponse block() {
    return this.serverResponse;
  }

  @Override
  public HttpStatusCode statusCode() {
    return this.serverResponse.statusCode();
  }

  @Override
  @Deprecated
  public int rawStatusCode() {
    return this.serverResponse.rawStatusCode();
  }

  @Override
  public HttpHeaders headers() {
    return this.serverResponse.headers();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    return this.serverResponse.cookies();
  }

  @Nullable
  @Override
  public Object writeTo(RequestContext request, Context context) throws Throwable {
    return this.serverResponse.writeTo(request, context);
  }

}
