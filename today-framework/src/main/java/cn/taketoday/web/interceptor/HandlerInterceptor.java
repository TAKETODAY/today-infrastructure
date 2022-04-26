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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.interceptor;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.RequestHandler;

/**
 * Handler process around Handler.
 *
 * @author TODAY 2018-06-25 20:06:11
 */
public interface HandlerInterceptor {
  /**
   * empty HandlerInterceptor array
   */
  HandlerInterceptor[] EMPTY_ARRAY = {};

  /**
   * NONE_RETURN_VALUE
   */
  Object NONE_RETURN_VALUE = RequestHandler.NONE_RETURN_VALUE;

  /**
   * Before Handler process.
   *
   * @param context Current request Context
   * @param handler Request handler
   * @return If is it possible to execute the target handler
   * @throws Throwable If any exception occurred
   */
  default boolean beforeProcess(RequestContext context, Object handler) throws Throwable {
    return true;
  }

  /**
   * After Handler processed.
   *
   * @param context Current request Context
   * @param handler Request handler
   * @param result Handler returned value
   * @throws Throwable If any exception occurred
   */
  default void afterProcess(RequestContext context, Object handler, Object result) throws Throwable { }

  /**
   * handler's interceptor intercept entrance
   *
   * @return return value is target handler's result
   * @since 4.0
   */
  default Object intercept(RequestContext context, InterceptorChain chain) throws Throwable {
    Object handler = chain.getHandler();
    if (beforeProcess(context, handler)) {
      Object result = chain.proceed(context);
      afterProcess(context, handler, result);
      return result;
    }
    return NONE_RETURN_VALUE;
  }

}
