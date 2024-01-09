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

package cn.taketoday.web.handler;

import java.io.IOException;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.NotFoundHandler;
import cn.taketoday.web.RequestContext;

/**
 * Process Handler not found
 *
 * @author TODAY 2019-12-20 19:15
 */
public class SimpleNotFoundHandler implements NotFoundHandler {

  /**
   * Process not found
   */
  @Override
  public Object handleNotFound(RequestContext request) throws IOException {
    logNotFound(request);

    request.sendError(HttpStatus.NOT_FOUND);
    return NONE_RETURN_VALUE;
  }

  public static void logNotFound(RequestContext context) {
    if (pageNotFoundLogger.isWarnEnabled()) {
      pageNotFoundLogger.warn("No mapping for {} {}", context.getMethodValue(), context.getRequestURI());
    }
  }

}
