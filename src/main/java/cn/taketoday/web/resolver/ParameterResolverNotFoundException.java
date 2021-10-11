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

import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.view.FrameworkConfigurationException;

/**
 * For {@link ParameterResolvingStrategy} NotFound Exception
 *
 * @author TODAY 2021/9/9 22:36
 * @see ParameterResolvingStrategy
 * @since 4.0
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ParameterResolverNotFoundException extends FrameworkConfigurationException {
  private static final long serialVersionUID = 1L;

  private final MethodParameter parameter;

  public ParameterResolverNotFoundException(MethodParameter parameter) {
    this(parameter, null, null);
  }

  public ParameterResolverNotFoundException(MethodParameter parameter, String message) {
    this(parameter, message, null);
  }

  public ParameterResolverNotFoundException(MethodParameter parameter, Throwable cause) {
    this(parameter, null, cause);
  }

  public ParameterResolverNotFoundException(MethodParameter parameter, String message, Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public MethodParameter getParameter() {
    return parameter;
  }

  public String getParameterName() {
    return parameter.getName();
  }

  public Class<?> getParameterClass() {
    return parameter.getParameterClass();
  }

}
