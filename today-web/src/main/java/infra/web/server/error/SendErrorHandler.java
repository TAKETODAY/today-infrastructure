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

package infra.web.server.error;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.web.RequestContext;

/**
 * An interface for handling errors that occur during the processing of HTTP requests.
 *
 * <p>This interface defines a method to manage error scenarios by processing the
 * provided {@code RequestContext} and an optional error message. Implementations can
 * define custom logic for logging errors, sending error responses, or performing
 * other error-handling tasks.</p>
 *
 * <p>Another common use case is integrating this interface with a controller class like
 * {@code BasicErrorController}, which handles errors in a web application context. The
 * {@code handleError} method can be invoked to process errors encountered during request handling.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BasicErrorController
 * @see RequestContext#sendError
 * @since 4.0 2023/7/27 18:01
 */
public interface SendErrorHandler {

  /**
   * Handles an error during the processing of an HTTP request.
   *
   * <p>This method is responsible for managing error scenarios by processing the
   * provided {@code RequestContext} and optional error message. It can be used to
   * log errors, send error responses, or perform other custom error-handling logic.</p>
   *
   * @param request the {@link RequestContext} representing the current HTTP request.
   * This object provides methods to send error responses or access
   * request-specific details.
   * @param message an optional error message describing the issue. If no message
   * is available, this parameter can be {@code null}.
   * @throws IOException if an I/O error occurs while handling the error, such as
   * when sending an error response to the client.
   */
  void handleError(RequestContext request, @Nullable String message) throws IOException;

}
