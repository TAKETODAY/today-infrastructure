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

package cn.taketoday.web;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.handler.SimpleNotFoundHandler;

/**
 * Process when handler not found
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/1/9 23:05
 */
public interface NotFoundHandler {

  /**
   * Log category to use when no mapped handler is found for a request.
   */
  String PAGE_NOT_FOUND_LOG_CATEGORY = "cn.taketoday.web.handler.PageNotFound";

  /** Additional logger to use when no mapped handler is found for a request. */
  Logger pageNotFoundLogger = LoggerFactory.getLogger(PAGE_NOT_FOUND_LOG_CATEGORY);

  /**
   * This value indicates that the handler did not return a value, or the result
   * has been processed
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * NotFoundHandler default instance
   */
  SimpleNotFoundHandler sharedInstance = new SimpleNotFoundHandler();

  @Nullable
  Object handleNotFound(RequestContext request) throws Throwable;

}
