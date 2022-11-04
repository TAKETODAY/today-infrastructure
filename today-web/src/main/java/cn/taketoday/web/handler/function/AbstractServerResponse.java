/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.function;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

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

  private final MultiValueMap<String, HttpCookie> cookies;

  protected AbstractServerResponse(HttpStatusCode statusCode,
          HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies) {
    this.statusCode = statusCode;
    this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
    this.cookies = MultiValueMap.unmodifiable(new LinkedMultiValueMap<>(cookies));
  }

  @Override
  public final HttpStatusCode statusCode() {
    return this.statusCode;
  }

  @Override
  @Deprecated
  public int rawStatusCode() {
    return this.statusCode.value();
  }

  @Override
  public final HttpHeaders headers() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, HttpCookie> cookies() {
    return this.cookies;
  }

  @Nullable
  @Override
  public Object writeTo(RequestContext request, Context context) throws Exception {
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
    context.mergeToResponse(headers);

    if (context.getResponseContentType() == null && headers.getFirst(HttpHeaders.CONTENT_TYPE) != null) {
      context.setContentType(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

  }

  private void writeCookies(RequestContext context) {
    for (Map.Entry<String, List<HttpCookie>> entry : cookies.entrySet()) {
      for (HttpCookie cookie : entry.getValue()) {
        context.addCookie(cookie);
      }
    }
  }

  @Nullable
  protected abstract Object writeToInternal(
          RequestContext request, Context context) throws Exception;

}
