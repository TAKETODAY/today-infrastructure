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

package cn.taketoday.web.service.invoker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link HttpServiceArgumentResolver} that resolves the target
 * request's HTTP method from an {@link HttpMethod} argument.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpMethodArgumentResolver implements HttpServiceArgumentResolver {

  private static final Log logger = LogFactory.getLog(HttpMethodArgumentResolver.class);

  @Override
  public boolean resolve(
          @Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    if (!parameter.getParameterType().equals(HttpMethod.class)) {
      return false;
    }

    Assert.notNull(argument, "HttpMethod is required");
    HttpMethod httpMethod = (HttpMethod) argument;
    requestValues.setHttpMethod(httpMethod);
    if (logger.isTraceEnabled()) {
      logger.trace("Resolved HTTP method to: " + httpMethod.name());
    }

    return true;
  }

}
