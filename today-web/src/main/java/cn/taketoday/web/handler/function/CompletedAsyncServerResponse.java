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

package cn.taketoday.web.handler.function;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RequestContext;

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
  public MultiValueMap<String, HttpCookie> cookies() {
    return this.serverResponse.cookies();
  }

  @Nullable
  @Override
  public Object writeTo(RequestContext request, Context context) throws Throwable {
    return this.serverResponse.writeTo(request, context);
  }

}
