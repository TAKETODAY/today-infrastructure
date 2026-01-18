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

import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.web.RequestContext;

/**
 * Abstract base class for {@link ServerResponse} implementations.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractServerResponse extends ErrorHandlingServerResponse {

  private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

  private final HttpStatusCode statusCode;

  private final HttpHeaders headers;

  @Nullable
  private final MultiValueMap<String, ResponseCookie> cookies;

  protected AbstractServerResponse(HttpStatusCode statusCode,
          HttpHeaders headers, @Nullable MultiValueMap<String, ResponseCookie> cookies) {
    this.statusCode = statusCode;
    this.headers = headers.asReadOnly();
    this.cookies = cookies;
  }

  @Override
  public final HttpStatusCode statusCode() {
    return this.statusCode;
  }

  @Override
  public int rawStatusCode() {
    return this.statusCode.value();
  }

  @Override
  public final HttpHeaders headers() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    if (cookies != null) {
      return this.cookies;
    }
    return MultiValueMap.empty();
  }

  @Nullable
  @Override
  public Object writeTo(RequestContext request, Context context) throws Throwable {
    try {
      writeStatusAndHeaders(request);

      if (SAFE_METHODS.contains(request.getMethod())
              && request.checkNotModified(headers().getETag(), headers().getLastModified())) {
        return NONE_RETURN_VALUE;
      }
      else {
        return writeToInternal(request, context);
      }
    }
    catch (Throwable throwable) {
      return handleError(throwable, request, context);
    }
  }

  private void writeStatusAndHeaders(RequestContext response) {
    response.setStatus(statusCode);
    writeHeaders(response);
    writeCookies(response);
  }

  private void writeHeaders(RequestContext context) {
    context.addHeaders(headers);

    if (context.getResponseContentType() == null && headers.getContentTypeAsString() != null) {
      context.setContentType(headers.getContentTypeAsString());
    }

  }

  private void writeCookies(RequestContext context) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (Map.Entry<String, List<ResponseCookie>> entry : cookies.entrySet()) {
        for (ResponseCookie cookie : entry.getValue()) {
          context.addCookie(cookie);
        }
      }
    }
  }

  @Nullable
  protected abstract Object writeToInternal(RequestContext request, Context context) throws Throwable;

}
