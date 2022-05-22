/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.registry;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.FunctionHandler;
import cn.taketoday.web.handler.RequestHandler;

import static cn.taketoday.http.HttpMethod.DELETE;
import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.http.HttpMethod.HEAD;
import static cn.taketoday.http.HttpMethod.OPTIONS;
import static cn.taketoday.http.HttpMethod.PATCH;
import static cn.taketoday.http.HttpMethod.POST;
import static cn.taketoday.http.HttpMethod.PUT;
import static cn.taketoday.http.HttpMethod.TRACE;

/**
 * @author TODAY <br>
 * 2019-12-26 17:33
 */
public class FunctionHandlerMapping extends MappedHandlerMapping {

  @Override
  protected String computeKey(final RequestContext context) {
    return context.getMethodValue().concat(context.getRequestPath());
  }

  // HEAD
  // ---------------------------------------

  public FunctionHandlerMapping head(String pathPattern, RequestHandler handler) {
    return HEAD(pathPattern, handler);
  }

  public FunctionHandlerMapping head(String pathPattern, FunctionHandler handler) {
    return HEAD(pathPattern, handler);
  }

  public FunctionHandlerMapping HEAD(String pathPattern, Object handler) {
    return register(HEAD, pathPattern, handler);
  }

  // PUT
  // ---------------------------------------

  public FunctionHandlerMapping put(String pathPattern, RequestHandler handler) {
    return PUT(pathPattern, handler);
  }

  public FunctionHandlerMapping put(String pathPattern, FunctionHandler handler) {
    return PUT(pathPattern, handler);
  }

  public FunctionHandlerMapping PUT(String pathPattern, Object handler) {
    return register(PUT, pathPattern, handler);
  }

  // DELETE
  // ---------------------------------------

  public FunctionHandlerMapping delete(String pathPattern, RequestHandler handler) {
    return DELETE(pathPattern, handler);
  }

  public FunctionHandlerMapping delete(String pathPattern, FunctionHandler handler) {
    return DELETE(pathPattern, handler);
  }

  public FunctionHandlerMapping DELETE(String pathPattern, Object handler) {
    return register(DELETE, pathPattern, handler);
  }
  // PATCH
  // ---------------------------------------

  public FunctionHandlerMapping patch(String pathPattern, RequestHandler handler) {
    return PATCH(pathPattern, handler);
  }

  public FunctionHandlerMapping patch(String pathPattern, FunctionHandler handler) {
    return PATCH(pathPattern, handler);
  }

  public FunctionHandlerMapping PATCH(String pathPattern, Object handler) {
    return register(PATCH, pathPattern, handler);
  }

  // TRACE
  // ---------------------------------------

  public FunctionHandlerMapping trace(String pathPattern, RequestHandler handler) {
    return TRACE(pathPattern, handler);
  }

  public FunctionHandlerMapping trace(String pathPattern, FunctionHandler handler) {
    return TRACE(pathPattern, handler);
  }

  public FunctionHandlerMapping TRACE(String pathPattern, Object handler) {
    return register(TRACE, pathPattern, handler);
  }

  // OPTIONS
  // ---------------------------------------

  public FunctionHandlerMapping options(String pathPattern, RequestHandler handler) {
    return OPTIONS(pathPattern, handler);
  }

  public FunctionHandlerMapping options(String pathPattern, FunctionHandler handler) {
    return OPTIONS(pathPattern, handler);
  }

  public FunctionHandlerMapping OPTIONS(String pathPattern, Object handler) {
    return register(OPTIONS, pathPattern, handler);
  }

  // POST
  // ---------------------------------------

  public FunctionHandlerMapping post(String pathPattern, RequestHandler handler) {
    return POST(pathPattern, handler);
  }

  public FunctionHandlerMapping post(String pathPattern, FunctionHandler handler) {
    return POST(pathPattern, handler);
  }

  public FunctionHandlerMapping POST(String pathPattern, Object handler) {
    return register(POST, pathPattern, handler);
  }

  // GET
  // ---------------------------------------

  public FunctionHandlerMapping get(String pathPattern, RequestHandler handler) {
    return GET(pathPattern, handler);
  }

  public FunctionHandlerMapping get(String pathPattern, FunctionHandler handler) {
    return GET(pathPattern, handler);
  }

  public FunctionHandlerMapping GET(String pathPattern, Object handler) {
    return register(GET, pathPattern, handler);
  }

  /**
   * Map a handler to the given URL path (or pattern)
   *
   * @param method Target HTTP request method
   * @param pathPattern Target path pattern
   * @param handler Handler object
   * @return This {@link FunctionHandlerMapping}
   */
  public FunctionHandlerMapping register(HttpMethod method, String pathPattern, Object handler) {
    registerHandler(method.name().concat(pathPattern), handler);
    return this;
  }

}
