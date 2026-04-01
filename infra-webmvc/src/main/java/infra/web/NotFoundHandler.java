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
 * Handler for processing requests when no appropriate handler is found.
 *
 * <p>This interface defines the contract for handling HTTP requests that do not match
 * any registered handler mappings. Implementations are responsible for generating
 * appropriate error responses (e.g., 404 Not Found) or performing custom fallback logic.</p>
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
   * Handles the request when no appropriate handler is found for the given path.
   *
   * <p>Implementations should generate an appropriate error response (typically HTTP 404)
   * or perform custom fallback logic. If the response has been fully processed within this
   * method, {@link #NONE_RETURN_VALUE} should be returned.</p>
   *
   * @param request the current request context containing request details and response access
   * @return the handler return value, or {@link #NONE_RETURN_VALUE} if no value is returned
   * or the response has already been processed
   * @throws Throwable if an error occurs during the handling process
   */
  @Nullable
  Object handleNotFound(RequestContext request) throws Throwable;

}
