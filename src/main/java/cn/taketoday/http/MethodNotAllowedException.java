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
package cn.taketoday.http;

import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY <br>
 * 2018-7-1 19:38:39
 */
@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class MethodNotAllowedException extends WebNestedRuntimeException {
  private static final long serialVersionUID = 1L;

  private final String requestMethod;
  private final HttpMethod[] supportedMethods;

  public MethodNotAllowedException(String requestMethod, HttpMethod[] supportedMethods) {
    super("Method '" + requestMethod + "' Not Allowed");
    this.requestMethod = requestMethod;
    this.supportedMethods = supportedMethods;
  }

  public String getMethod() {
    return requestMethod;
  }

  public HttpMethod getRequestMethod() {
    return HttpMethod.valueOf(requestMethod);
  }

  public HttpMethod[] getSupportedMethods() {
    return supportedMethods;
  }
}
