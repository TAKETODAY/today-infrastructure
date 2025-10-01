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

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import infra.http.HttpStatus;
import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2020/12/23 20:12
 */
public class HttpStatusReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturn(HttpStatus.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof HttpStatus;
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) {
    if (returnValue instanceof HttpStatus status) {
      context.setStatus(status);
    }
  }

}
