/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.handler;

import cn.taketoday.web.RequestContext;

/**
 * Handle Exception from handler
 *
 * @author TODAY <br>
 * 2020-03-29 20:52
 */
public interface HandlerExceptionHandler {
  String KEY_THROWABLE = HandlerExceptionHandler.class.getName() + "-context-throwable";

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = HandlerAdapter.NONE_RETURN_VALUE;

  /**
   * Handle exception
   *
   * @param exception The exception occurred
   * @param handler Current handler
   * @return Exception view
   * @throws Throwable If any {@link Exception} occurred
   */
  Object handleException(RequestContext context, Throwable exception, Object handler)
          throws Throwable;

}
