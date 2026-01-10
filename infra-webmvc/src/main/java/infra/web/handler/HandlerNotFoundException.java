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

package infra.web.handler;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.web.DispatcherHandler;
import infra.web.ErrorResponse;
import infra.web.InfraConfigurationException;

/**
 * By default when the DispatcherHandler can't find a handler for a request it
 * sends a 404 response. However if its property "throwExceptionIfNoHandlerFound"
 * is set to {@code true} this exception is raised and may be handled with
 * a configured HandlerExceptionHandler.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DispatcherHandler#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherHandler#handlerNotFound(infra.web.RequestContext)
 * @since 4.0 2022/1/28 23:19
 */
public class HandlerNotFoundException extends InfraConfigurationException implements ErrorResponse {

  private final String httpMethod;

  private final String requestURI;

  private final HttpHeaders requestHeaders;

  /**
   * Constructor for HandlerNotFoundException.
   *
   * @param httpMethod the HTTP method
   * @param requestURI the HTTP request URI
   * @param headers the HTTP request headers
   */
  public HandlerNotFoundException(String httpMethod, String requestURI, HttpHeaders headers) {
    super("No endpoint %s %s.".formatted(httpMethod, requestURI));
    this.httpMethod = httpMethod;
    this.requestURI = requestURI;
    this.requestHeaders = headers;
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.NOT_FOUND;
  }

  public String getHttpMethod() {
    return this.httpMethod;
  }

  public String getRequestURI() {
    return this.requestURI;
  }

  /**
   * Return the headers of the request.
   */
  public HttpHeaders getRequestHeaders() {
    return this.requestHeaders;
  }

}
