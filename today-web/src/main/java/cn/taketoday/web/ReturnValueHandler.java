/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;

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
 */
public interface ReturnValueHandler {

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

  /**
   * Multiple ReturnValueHandlers under the ReturnValueHandler system choose different
   * ReturnValueHandlers corresponding to different {@code handler} and {@code returnValue}
   *
   * @param handlers Multiple ReturnValueHandlers
   * @param handler Request handler
   * @param returnValue handler's result ,if returnValue is
   * {@link ReturnValueHandler#NONE_RETURN_VALUE} match handler only
   * @return A ReturnValueHandler that matches the situation ,
   * null if returnValue is {@link ReturnValueHandler#NONE_RETURN_VALUE} or no one matched
   */
  @Nullable
  static ReturnValueHandler select(List<ReturnValueHandler> handlers,
          @Nullable Object handler, @Nullable Object returnValue) {
    if (returnValue != NONE_RETURN_VALUE) {
      if (handler != null) {
        // match handler and return-value
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler instanceof SmartReturnValueHandler smartHandler) {
            // smart handler
            if (smartHandler.supportsHandler(handler, returnValue)) {
              return returnValueHandler;
            }
          }
          else {
            if (returnValueHandler.supportsHandler(handler)
                    || returnValueHandler.supportsReturnValue(returnValue)) {
              return returnValueHandler;
            }
          }
        }
      }
      else {
        // match return-value only
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler.supportsReturnValue(returnValue)) {
            return returnValueHandler;
          }
        }
      }
    }
    else if (handler != null) {
      // match handler only
      for (ReturnValueHandler returnValueHandler : handlers) {
        if (returnValueHandler.supportsHandler(handler)) {
          return returnValueHandler;
        }
      }
    }
    return null;
  }

}
