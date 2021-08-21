/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.view;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExceptionHandler;

/**
 * ResultHandler
 *
 * <p>
 * Handle handler execution result
 *
 * @author TODAY <br>
 * 2019-07-10 19:22
 */
@FunctionalInterface
public interface ResultHandler {
  String RESPONSE_BODY_PREFIX = "body:";
  String REDIRECT_URL_PREFIX = "redirect:";

  /**
   * If this {@link ResultHandler} supports the target handler
   * <p>
   * This method can test this {@link ResultHandler} supports the target handler
   * in application startup time
   *
   * @param handler
   *         Target HTTP handler
   *
   * @return If this {@link ResultHandler} supports the target handler
   */
  default boolean supportsHandler(Object handler) {
    return true;
  }

  /**
   * Handle result of the handler
   *
   * @param context
   *         Current HTTP request context
   * @param handler
   *         Target HTTP handler
   * @param result
   *         Handler execution result
   *         Or {@link HandlerExceptionHandler} return value
   */
  void handleResult(RequestContext context, Object handler, Object result) throws Throwable;

}
