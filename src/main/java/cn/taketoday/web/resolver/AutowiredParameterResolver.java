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

package cn.taketoday.web.resolver;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2021/4/2 23:21
 * @since 3.0
 */
public class AutowiredParameterResolver implements ParameterResolver {
  private final WebApplicationContext context;

  public AutowiredParameterResolver(WebApplicationContext context) {
    this.context = context;
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isAnnotationPresent(Autowired.class);
  }

  @Override
  public Object resolveParameter(final RequestContext ctx, final MethodParameter parameter) throws Throwable {
    final Autowired autowired = parameter.getAnnotation(Autowired.class);
    final Object bean;
    final String name = autowired.value();
    if (StringUtils.isEmpty(name)) {
      bean = context.getBean(parameter.getParameterClass());
    }
    else {
      bean = context.getBean(name, parameter.getParameterClass());
    }
    if (bean == null && autowired.required()) {
      throw new NoSuchBeanDefinitionException(parameter.getParameterClass());
    }
    return bean;
  }
}
