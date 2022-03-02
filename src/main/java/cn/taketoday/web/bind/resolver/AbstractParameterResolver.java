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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.web.bind.MissingRequestValueException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/3/10 19:59
 * @since 3.0
 */
public abstract class AbstractParameterResolver implements ParameterResolvingStrategy {

  /**
   * @param context Current request Context
   * @param resolvable parameter
   * @return parameter value
   * @throws MissingRequestValueException parameter cannot be resolved
   */
  @Override
  public Object resolveParameter(
          final RequestContext context, final ResolvableMethodParameter resolvable) throws Throwable {
    final Object parameterValue = resolveInternal(context, resolvable);
    if (parameterValue == null) {
      if (resolvable.isRequired()) {
        return missingParameter(resolvable);
      }
      else {
        return fromDefaultValue(context, resolvable.getDefaultValue());
      }
    }
    else {
      return transformValue(context, resolvable, parameterValue);
    }
  }

  /**
   * subclasses can perform type conversion
   *
   * @param original original value
   */
  protected Object transformValue(RequestContext context, ResolvableMethodParameter parameter, Object original) {
    return original;
  }

  /**
   * @return null-able parameter value
   */
  protected abstract Object resolveInternal(final RequestContext context, final ResolvableMethodParameter parameter) throws Throwable;

  protected Object fromDefaultValue(RequestContext context, String defaultValue) throws Throwable {
    return null;
  }

  /**
   * handle missed parameter value
   */
  protected Object missingParameter(final ResolvableMethodParameter parameter) {
    throw new MissingRequestValueException("Required parameter '" + parameter.getParameterName() + "' is not present");
  }

}
