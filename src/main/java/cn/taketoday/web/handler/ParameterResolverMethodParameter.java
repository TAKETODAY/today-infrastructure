/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;

/**
 * @author TODAY
 * @date 2020/9/26 20:06
 * @since 3.0
 */
public class ParameterResolverMethodParameter extends MethodParameter {

  private final ParameterResolver resolver = ParameterResolvers.obtainResolver(this);

  public ParameterResolverMethodParameter(HandlerMethod handler, MethodParameter other) {
    super(handler, other);
  }

  public ParameterResolverMethodParameter(int index, Parameter parameter, String parameterName) {
    super(index, parameter, parameterName);
  }

  @Override
  protected Object resolveParameter(final RequestContext request) throws Throwable {
    return resolver.resolveParameter(request, this);
  }

  public ParameterResolver getResolver() {
    return resolver;
  }

  // static
  // --------------------------------------

  public static MethodParameter[] ofMethod(Method method) {
    final int length = method.getParameterCount();
    if (length == 0) {
      return null;
    }

    final MethodParameter[] ret = new MethodParameter[length];
    final String[] methodArgsNames = ClassUtils.getMethodArgsNames(method);
    final Parameter[] parameters = method.getParameters();
    for (int i = 0; i < length; i++) {
      ret[i] = new ParameterResolverMethodParameter(i, parameters[i], methodArgsNames[i]);
    }
    return ret;
  }

}
