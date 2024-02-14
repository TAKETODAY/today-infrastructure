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

package cn.taketoday.web.handler.result;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * just for HandlerMethod return-value handling
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-13 13:52
 */
public interface HandlerMethodReturnValueHandler extends ReturnValueHandler {

  @Override
  default boolean supportsHandler(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return supportsHandlerMethod(handlerMethod);
    }
    return false;
  }

  /**
   * Whether the given {@linkplain HandlerMethod method} is supported by this handler.
   *
   * @return {@code true} if this handler supports the supplied return type;
   * {@code false} otherwise
   * @see HandlerMethod
   */
  default boolean supportsHandlerMethod(HandlerMethod handler) {
    return false;
  }

  /**
   * Handle result of the handler
   *
   * @param context Current HTTP request context
   * @param handler handler may be HandlerMethod
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws Exception return-value handled failed
   */
  @Override
  default void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      handleHandlerMethodReturnValue(context, handlerMethod, returnValue);
    }
  }

  /**
   * Handle result of the handler
   *
   * @param context Current HTTP request context
   * @param handler HandlerMethod
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws Exception return-value handled failed
   */
  default void handleHandlerMethodReturnValue(
          RequestContext context, HandlerMethod handler, @Nullable Object returnValue) throws Exception {

  }

}
