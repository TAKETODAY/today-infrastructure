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

package cn.taketoday.web.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.support.DefaultParameterNameDiscoverer;

/**
 * Build {@link MethodParameter} array
 *
 * @author TODAY 2021/3/21 13:58
 * @since 3.0
 */
public class MethodParametersBuilder {
  private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Nullable
  public MethodParameter[] build(Method method) {
    final int length = method.getParameterCount();
    if (length == 0) {
      return null;
    }
    final MethodParameter[] ret = new MethodParameter[length];
    final String[] methodArgsNames = parameterNameDiscoverer.getParameterNames(method);
    if (methodArgsNames != null) {
      final Parameter[] parameters = method.getParameters();
      for (int i = 0; i < length; i++) {
        ret[i] = createParameter(methodArgsNames[i], parameters[i], i);
      }
    }else{
    // TODO
    }
    return ret;
  }

  protected MethodParameter createParameter(String methodArgsName, Parameter parameter, int index) {
    return new MethodParameter(index, parameter, methodArgsName);
  }

  public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parameterNameDiscoverer, "parameterNameDiscoverer must not be null");
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return parameterNameDiscoverer;
  }
}
