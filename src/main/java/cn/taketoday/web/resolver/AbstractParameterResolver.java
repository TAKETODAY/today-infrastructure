/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2021/3/10 19:59
 * @since 3.0
 */
public abstract class AbstractParameterResolver implements ParameterResolver {

  /**
   * @param context
   *         Current request Context
   * @param parameter
   *         parameter
   *
   * @return parameter value
   *
   * @throws MissingParameterException
   *         parameter cannot be resolved
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Object parameterValue = resolveInternal(context, parameter);
    if (parameterValue == null) {
      if (parameter.isRequired()) {
        return missingParameter(parameter);
      }
      else {
        return fromDefaultValue(context, parameter.getDefaultValue());
      }
    }
    else {
      return transformValue(context, parameter, parameterValue);
    }
  }

  /**
   * subclasses can perform type conversion
   *
   * @param original
   *         original value
   */
  protected Object transformValue(RequestContext context, MethodParameter parameter, Object original) {
    return original;
  }

  /**
   * @return null-able parameter value
   */
  protected abstract Object resolveInternal(final RequestContext context, final MethodParameter parameter) throws Throwable;

  protected Object fromDefaultValue(RequestContext context, String defaultValue) throws Throwable {
    return null;
  }

  /**
   * handle missed parameter value
   */
  protected Object missingParameter(final MethodParameter parameter) {
    throw new MissingParameterException(parameter);
  }

}
