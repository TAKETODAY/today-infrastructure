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

import cn.taketoday.lang.Nullable;

/**
 * Plain handler interface for components that process HTTP requests
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ReturnValueHandler
 * @since 2019-12-21 17:37
 */
@FunctionalInterface
public interface HttpRequestHandler {

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = new Object();

  /**
   * Process the request and return a result object which the DispatcherHandler
   * will handle. A {@code null} return value is not an error: it indicates that
   * this handler completed request processing itself and that there is therefore no
   * explicit result to handle. a {@link #NONE_RETURN_VALUE} indicates that no
   * result to handle by {@link ReturnValueHandler}
   *
   * @param request Current request context
   * @return Result to be handled by {@link ReturnValueHandler}
   * @throws Throwable If any exception occurred
   * @see ReturnValueHandler
   */
  @Nullable
  Object handleRequest(RequestContext request) throws Throwable;

}
