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
import cn.taketoday.web.view.FrameworkConfigurationException;

/**
 * By default when the DispatcherHandler can't find a handler for a request it
 * sends a 404 response. However if its property "throwExceptionIfNoHandlerFound"
 * is set to {@code true} this exception is raised and may be handled with
 * a configured HandlerExceptionHandler.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DispatcherHandler#setThrowExceptionIfNoHandlerFound(boolean)
 * @see DispatcherHandler#noHandlerFound(cn.taketoday.web.RequestContext)
 * @since 4.0 2022/1/28 23:19
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends FrameworkConfigurationException {

  private final String httpMethod;

  private final String requestURL;

  private final HttpHeaders headers;

  /**
   * Constructor for NoHandlerFoundException.
   *
   * @param httpMethod the HTTP method
   * @param requestURL the HTTP request URL
   * @param headers the HTTP request headers
   */
  public NoHandlerFoundException(String httpMethod, String requestURL, HttpHeaders headers) {
    super("No handler found for " + httpMethod + " " + requestURL);
    this.httpMethod = httpMethod;
    this.requestURL = requestURL;
    this.headers = headers;
  }

  public String getHttpMethod() {
    return this.httpMethod;
  }

  public String getRequestURL() {
    return this.requestURL;
  }

  public HttpHeaders getHeaders() {
    return this.headers;
  }

}
