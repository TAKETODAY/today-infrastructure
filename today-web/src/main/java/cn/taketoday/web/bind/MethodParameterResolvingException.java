/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.web.bind;

import cn.taketoday.core.MethodParameter;

/**
 * MethodParameter can't be resolved
 *
 * @author TODAY 2021/1/17 10:05
 * @since 3.0
 */
public class MethodParameterResolvingException extends RequestBindingException {

  private final MethodParameter parameter;

  public MethodParameterResolvingException(MethodParameter parameter) {
    this(parameter, null, null);
  }

  public MethodParameterResolvingException(MethodParameter parameter, String message) {
    this(parameter, message, null);
  }

  public MethodParameterResolvingException(MethodParameter parameter, Throwable cause) {
    this(parameter, null, cause);
  }

  public MethodParameterResolvingException(MethodParameter parameter, String message, Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public MethodParameter getParameter() {
    return parameter;
  }

  public String getParameterName() {
    return parameter.getParameterName();
  }

  public Class<?> getParameterType() {
    return parameter.getParameterType();
  }
}
