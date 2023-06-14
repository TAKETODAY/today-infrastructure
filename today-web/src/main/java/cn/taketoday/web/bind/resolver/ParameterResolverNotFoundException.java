/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.io.Serial;

import cn.taketoday.web.InfraConfigurationException;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * For {@link ParameterResolvingStrategy} NotFound Exception
 *
 * @author TODAY 2021/9/9 22:36
 * @see ParameterResolvingStrategy
 * @since 4.0
 */
public class ParameterResolverNotFoundException extends InfraConfigurationException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ResolvableMethodParameter parameter;

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter) {
    this(parameter, null, null);
  }

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter, String message) {
    this(parameter, message, null);
  }

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter, Throwable cause) {
    this(parameter, null, cause);
  }

  public ParameterResolverNotFoundException(ResolvableMethodParameter parameter, String message, Throwable cause) {
    super(message, cause);
    this.parameter = parameter;
  }

  public ResolvableMethodParameter getParameter() {
    return parameter;
  }

  public String getParameterName() {
    return parameter.getName();
  }

  public Class<?> getParameterClass() {
    return parameter.getParameterType();
  }

}
