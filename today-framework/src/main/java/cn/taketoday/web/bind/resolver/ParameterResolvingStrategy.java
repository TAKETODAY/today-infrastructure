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

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MethodParameterResolvingException;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Strategy interface for method parameter resolving.
 * <p>
 * run in RequestContext
 * </p>
 *
 * @author TODAY 2019-07-07 23:24
 * @see ResolvableMethodParameter
 * @see MethodParameterResolvingException
 */
public interface ParameterResolvingStrategy {

  /**
   * Whether the given parameter is supported by this resolver.
   * <p>
   * static match
   * </p>
   */
  boolean supportsParameter(ResolvableMethodParameter resolvable);

  /**
   * Resolve parameter
   *
   * @param context Current request context
   * @param resolvable resolvable parameter
   * @return method parameter instance
   * @throws Throwable if any {@link Exception} occurred
   * @see MethodParameterResolvingException
   */
  @Nullable
  Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable;

  @FunctionalInterface
  interface SupportsFunction {

    boolean supports(ResolvableMethodParameter parameter);
  }

  record TargetSupportsFunction(Class<?> targetType) implements SupportsFunction {

    @Override
    public boolean supports(ResolvableMethodParameter parameter) {
      return parameter.is(targetType);
    }

  }
}
