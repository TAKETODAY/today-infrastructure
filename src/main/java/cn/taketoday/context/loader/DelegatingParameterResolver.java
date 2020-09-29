/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.factory.BeanFactory;

/**
 * @author TODAY <br>
 * 2019-10-28 20:27
 */
public final class DelegatingParameterResolver
        extends OrderedSupport implements ExecutableParameterResolver, Ordered {

  private final SupportsFunction supports;
  private final ExecutableParameterResolver resolver;

  public DelegatingParameterResolver(SupportsFunction supports, ExecutableParameterResolver resolver) {
    this(supports, resolver, Ordered.HIGHEST_PRECEDENCE);
  }

  public DelegatingParameterResolver(SupportsFunction supports, ExecutableParameterResolver resolver, int order) {
    super(order);
    this.resolver = resolver;
    this.supports = supports;
  }

  @Override
  public boolean supports(Parameter parameter) {
    return supports.supports(parameter);
  }

  @Override
  public Object resolve(Parameter parameter, BeanFactory beanFactory) {
    return resolver.resolve(parameter, beanFactory);
  }

  public static DelegatingParameterResolver delegate(SupportsFunction supports, ExecutableParameterResolver resolver) {
    return new DelegatingParameterResolver(supports, resolver);
  }

  public static DelegatingParameterResolver delegate(SupportsFunction supports,
                                                     ExecutableParameterResolver resolver, int order) {
    return new DelegatingParameterResolver(supports, resolver, order);
  }

}
