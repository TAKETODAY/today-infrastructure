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
