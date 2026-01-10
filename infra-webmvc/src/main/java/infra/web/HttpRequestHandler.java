/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
