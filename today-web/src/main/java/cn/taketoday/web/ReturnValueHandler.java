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

package cn.taketoday.web;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * handler return-value Handler
 *
 * <p>
 * Handle request-handler execution result
 * <p>
 * ReturnValueHandler is HTTP response writer
 * </p>
 *
 * <p>
 * <b>Note:</b> This framework allows
 * request-handler implement this interface to handle its execution result
 *
 * @author TODAY 2019-07-10 19:22
 * @see HandlerExceptionHandler
 * @see HandlerMethod
 * @see ReturnValueHandlerProvider
 */
public interface ReturnValueHandler {
  String REDIRECT_URL_PREFIX = "redirect:";

  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * If this {@link ReturnValueHandler} supports the target handler
   * <p>
   * This method can test this {@link ReturnValueHandler} supports the target handler
   * in application startup time , static match
   *
   * @param handler Target HTTP handler
   * @return If this {@link ReturnValueHandler} supports the target handler
   */
  boolean supportsHandler(Object handler);

  /**
   * If this {@link ReturnValueHandler} supports the target handler's result
   * <p>
   * This method can test this {@link ReturnValueHandler} supports the target handler
   * in application runtime
   *
   * @param returnValue Target handler's return-value or result
   * @return If this {@link ReturnValueHandler} supports the target handler's result
   * @since 4.0
   */
  default boolean supportsReturnValue(@Nullable Object returnValue) {
    return false;
  }

  /**
   * Handle result of the handler
   *
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws Exception return-value handled failed
   */
  void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue)
          throws Exception;

}
