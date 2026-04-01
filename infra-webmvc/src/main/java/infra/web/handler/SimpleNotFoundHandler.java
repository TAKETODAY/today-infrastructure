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

package infra.web.handler;

import java.io.IOException;

import infra.http.HttpStatus;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;

/**
 * A simple implementation of {@link NotFoundHandler} that handles requests
 * where no matching handler was found.
 * <p>
 * This implementation logs a warning message and sends an HTTP 404 (Not Found)
 * error response to the client.
 *
 * @author TODAY 2019-12-20 19:15
 */
public class SimpleNotFoundHandler implements NotFoundHandler {

  /**
   * Handles the situation where no handler is found for the given request.
   * <p>
   * This method logs the missing mapping and sends an HTTP 404 error response.
   *
   * @param request the current request context
   * @return {@code null} as there is no return value for error handling
   * @throws IOException if an I/O error occurs while sending the error response
   */
  @Override
  public Object handleNotFound(RequestContext request) throws IOException {
    logNotFound(request);

    request.sendError(HttpStatus.NOT_FOUND);
    return NONE_RETURN_VALUE;
  }

  /**
   * Logs a warning message indicating that no mapping was found for the request.
   * <p>
   * The log message includes the HTTP method and the request URI.
   *
   * @param context the request context containing method and URI information
   */
  public static void logNotFound(RequestContext context) {
    if (pageNotFoundLogger.isWarnEnabled()) {
      pageNotFoundLogger.warn("No mapping for {} {}", context.getMethodAsString(), context.getRequestURI());
    }
  }

}
