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

package infra.web.handler.result;

import infra.http.HttpHeaders;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;

/**
 * Handles return values of type {@link HttpHeaders}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 11:00
 */
public class HttpHeadersReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturn(HttpHeaders.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof HttpHeaders;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) {
    if (returnValue instanceof HttpHeaders headers && !headers.isEmpty()) {
      context.mergeToResponse(headers);
    }
  }

}
