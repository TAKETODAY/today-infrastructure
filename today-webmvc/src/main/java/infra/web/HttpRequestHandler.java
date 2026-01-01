/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web;

import org.jspecify.annotations.Nullable;

/**
 * Represents a interface for handling HTTP requests. This interface
 * is typically used in web frameworks to process incoming requests and return
 * results that can be further handled by the framework.
 *
 * <p>The {@link #handleRequest(RequestContext)} method is the core of this interface.
 * It processes the request and returns an object that represents the result of
 * the processing. The returned object can be explicitly handled by a
 * {@link ReturnValueHandler} or indicate that no further processing is needed.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 *  // Example 1: A simple handler that returns a string response
 *  HttpRequestHandler handler = request -> {
 *    String responseBody = "Hello, World!";
 *    return ResponseEntity.ok(responseBody);
 *  };
 * }</pre>
 *
 * <pre>{@code
 *  // Example 2: A handler that processes the request and marks it as completed
 *  HttpRequestHandler handler = request -> {
 *    // Perform some business logic
 *    if (request.getParameter("action").equals("logout")) {
 *      request.getSession().invalidate();
 *      return HttpRequestHandler.NONE_RETURN_VALUE; // No further processing needed
 *    }
 *    return null; // Let the framework handle the result
 *  };
 * }</pre>
 *
 * <p><b>Special Return Values:</b>
 * <ul>
 *   <li>{@code null}: Indicates that the handler has completed processing,
 *       but the framework should still handle the result.</li>
 *   <li>{@link #NONE_RETURN_VALUE}: Indicates that the handler has fully processed
 *       the request and no further result handling is required.</li>
 * </ul>
 *
 * @see ReturnValueHandler
 * @see RequestContext
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
