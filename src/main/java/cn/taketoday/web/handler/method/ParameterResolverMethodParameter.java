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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler.method;

import java.lang.reflect.Parameter;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2020/9/26 20:06
 * @since 3.0
 */
public class ParameterResolverMethodParameter extends ResolvableMethodParameter {
  private final ParameterResolvingRegistry resolvers;
  private ParameterResolvingStrategy resolver;

  public ParameterResolverMethodParameter(
          HandlerMethod handler, ResolvableMethodParameter other, ParameterResolvingRegistry resolvers) {
    super(handler, other);
    this.resolvers = resolvers;
  }

  public ParameterResolverMethodParameter(
          int index, Parameter parameter, String parameterName, ParameterResolvingRegistry resolvers) {
    super(index, parameter, parameterName);
    this.resolvers = resolvers;
  }

  @Override
  protected Object resolveParameter(final RequestContext request) throws Throwable {
    return obtainResolver().resolveParameter(request, this);
  }

  public final ParameterResolvingStrategy obtainResolver() {
    if (resolver == null) {
      resolver = resolvers.obtainResolvingStrategy(this);
    }
    return resolver;
  }

}
