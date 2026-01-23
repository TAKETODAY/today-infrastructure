/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.handler.SimpleNotFoundHandler;

/**
 * Handler for processing requests when no appropriate handler is found
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/1/9 23:05
 */
public interface NotFoundHandler {

  /**
   * Log category to use when no mapped handler is found for a request.
   */
  String PAGE_NOT_FOUND_LOG_CATEGORY = "infra.web.handler.PageNotFound";

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

  /**
   * Handle the request when no appropriate handler is found
   *
   * @param request current request context
   * @return handler return value or {@link #NONE_RETURN_VALUE} if no value returned or already processed
   * @throws Throwable in case of errors during handling
   */
  @Nullable
  Object handleNotFound(RequestContext request) throws Throwable;

}
