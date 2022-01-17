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

package cn.taketoday.web.resolver;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/4/2 23:21
 * @since 3.0
 */
public class AutowiredParameterResolver
        extends AbstractParameterResolver implements ParameterResolvingStrategy {
  private final AutowireCapableBeanFactory beanFactory;

  public AutowiredParameterResolver(WebApplicationContext context) {
    this.beanFactory = context.getAutowireCapableBeanFactory();
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAnnotationPresent(Autowired.class);
  }

  @Override
  protected Object missingParameter(ResolvableMethodParameter parameter) {
    throw new NoSuchBeanDefinitionException(parameter.getParameterType());
  }

  @Override
  protected Object resolveInternal(RequestContext ctx, ResolvableMethodParameter parameter) throws Throwable {
    return beanFactory.resolveDependency(new DependencyDescriptor(parameter.getParameter(), true), parameter.getName());
  }
}
