/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.InfraConfigurationException;

/**
 * By default when the DispatcherHandler can't find a handler for a request it
 * sends a 404 response. However if its property "throwExceptionIfNoHandlerFound"
 * is set to {@code true} this exception is raised and may be handled with
 * a configured HandlerExceptionHandler.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DispatcherHandler#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherHandler#handlerNotFound(cn.taketoday.web.RequestContext)
 * @since 4.0 2022/1/28 23:19
 */
@SuppressWarnings("serial")
public class HandlerNotFoundException extends InfraConfigurationException implements ErrorResponse {

  private final String httpMethod;

  private final String requestURI;

  private final HttpHeaders headers;

  private final ProblemDetail body;

  /**
   * Constructor for NoHandlerFoundException.
   *
   * @param httpMethod the HTTP method
   * @param requestURI the HTTP request URI
   * @param headers the HTTP request headers
   */
  public HandlerNotFoundException(String httpMethod, String requestURI, HttpHeaders headers) {
    super("No endpoint " + httpMethod + " " + requestURI + ".");
    this.httpMethod = httpMethod;
    this.requestURI = requestURI;
    this.headers = headers;
    this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
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

  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

}
