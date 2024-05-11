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

package cn.taketoday.web.handler.method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2020/9/26 20:06
 * @since 3.0
 */
final class ParameterResolverMethodParameter extends ResolvableMethodParameter {

  private final ParameterResolvingRegistry resolvers;

  @Nullable
  private ParameterResolvingStrategy strategy;

  ParameterResolverMethodParameter(MethodParameter parameter, ParameterResolvingRegistry resolvers) {
    super(parameter);
    this.resolvers = resolvers;
  }

  ParameterResolverMethodParameter(ResolvableMethodParameter other, MethodParameter parameter, ParameterResolvingRegistry resolvers) {
    super(other, parameter);
    this.resolvers = resolvers;
  }

  @Override
  @Nullable
  public Object resolveParameter(final RequestContext request) throws Throwable {
    ParameterResolvingStrategy strategy = this.strategy;
    if (strategy == null) {
      strategy = resolvers.obtainStrategy(this);
      this.strategy = strategy;
    }
    return strategy.resolveArgument(request, this);
  }

  @Override
  protected ResolvableMethodParameter nested(MethodParameter parameter) {
    return new ParameterResolverMethodParameter(this, parameter, resolvers);
  }

}
