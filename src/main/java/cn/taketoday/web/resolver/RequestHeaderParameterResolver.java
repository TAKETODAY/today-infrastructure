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
package cn.taketoday.web.resolver;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.handler.MethodParameter;

/**
 * for {@link RequestHeader}
 *
 * @author TODAY <br>
 * 2019-07-13 11:11
 */
public class RequestHeaderParameterResolver extends ConversionServiceParameterResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.isAnnotationPresent(RequestHeader.class);
  }

  @Override
  protected Object missingParameter(MethodParameter parameter) {
    throw new MissingRequestHeaderException(parameter);
  }

  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) {
    final String headerName = parameter.getName();
    final HttpHeaders httpHeaders = context.requestHeaders();
    return httpHeaders.get(headerName);
  }
}
