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
package cn.taketoday.beans.autowire;

import java.lang.reflect.Parameter;

import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;

/**
 * @author TODAY <br>
 * 2019-10-28 20:27
 */
public final class DelegatingArgumentsResolver
        extends OrderedSupport implements ArgumentsResolvingStrategy, Ordered {

  private final SupportsFunction supports;
  private final ArgumentsResolvingStrategy resolver;

  public DelegatingArgumentsResolver(SupportsFunction supports, ArgumentsResolvingStrategy resolver) {
    this(supports, resolver, Ordered.HIGHEST_PRECEDENCE);
  }

  public DelegatingArgumentsResolver(SupportsFunction supports, ArgumentsResolvingStrategy resolver, int order) {
    super(order);
    this.resolver = resolver;
    this.supports = supports;
  }

  @Override
  public boolean supports(Parameter parameter, BeanFactory beanFactory) {
    return supports.supports(parameter);
  }

  @Override
  public Object resolve(Parameter parameter, BeanFactory beanFactory) {
    return resolver.resolve(parameter, beanFactory);
  }

  public static DelegatingArgumentsResolver delegate(SupportsFunction supports, ArgumentsResolvingStrategy resolver) {
    return new DelegatingArgumentsResolver(supports, resolver);
  }

  public static DelegatingArgumentsResolver delegate(SupportsFunction supports,
                                                     ArgumentsResolvingStrategy resolver, int order) {
    return new DelegatingArgumentsResolver(supports, resolver, order);
  }

}
