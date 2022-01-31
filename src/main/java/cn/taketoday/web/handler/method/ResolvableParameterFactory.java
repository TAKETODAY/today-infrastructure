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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Build {@link ResolvableMethodParameter} array
 *
 * @author TODAY 2021/3/21 13:58
 * @since 3.0
 */
public class ResolvableParameterFactory {
  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  public ResolvableMethodParameter[] createArray(Method method) {
    final int length = method.getParameterCount();
    if (length == 0) {
      return null;
    }
    final ResolvableMethodParameter[] ret = new ResolvableMethodParameter[length];
    for (int i = 0; i < length; i++) {
      MethodParameter parameter = new SynthesizingMethodParameter(method, i);
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
      ret[i] = createParameter(parameter);
    }

    return ret;
  }

  @Nullable
  public ResolvableMethodParameter[] createArray(HandlerMethod handlerMethod) {
    MethodParameter[] parameters = handlerMethod.getParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return null;
    }
    int i = 0;
    ResolvableMethodParameter[] ret = new ResolvableMethodParameter[parameters.length];
    for (MethodParameter parameter : parameters) {
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
      ret[i] = createParameter(parameter);
      i++;
    }
    return ret;
  }

  protected ResolvableMethodParameter createParameter(MethodParameter parameter) {
    return new ResolvableMethodParameter(parameter);
  }

  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parameterNameDiscoverer, "parameterNameDiscoverer must not be null");
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return parameterNameDiscoverer;
  }
}
