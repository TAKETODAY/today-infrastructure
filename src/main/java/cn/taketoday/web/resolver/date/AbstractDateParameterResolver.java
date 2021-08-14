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

package cn.taketoday.web.resolver.date;

import cn.taketoday.context.EmptyObject;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.DateTimeFormat;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ParameterResolver;

/**
 * @author TODAY 2021/3/2 12:30
 * @since 3.0
 */
public class AbstractDateParameterResolver
        extends OrderedSupport implements ParameterResolver {
  static final String FORMAT_ANNOTATION_KEY = AbstractDateParameterResolver.class.getName() + "-DateTimeFormat";

  @Override
  public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
    final String parameterValue = getParameterValue(context, parameter);
    if (StringUtils.isEmpty(parameterValue)) {
      return null;
    }
    return resolveInternal(parameterValue, parameter);
  }

  protected Object resolveInternal(String parameterValue, MethodParameter parameter) {
    return null;
  }

  protected String getParameterValue(RequestContext context, MethodParameter parameter) {
    return context.getParameter(parameter.getName());
  }

  protected DateTimeFormat getAnnotation(MethodParameter parameter) {
    final Object attribute = parameter.getAttribute(FORMAT_ANNOTATION_KEY);
    if (attribute == null) {
      DateTimeFormat ret = parameter.getAnnotation(DateTimeFormat.class);
      if (ret == null) {
        final HandlerMethod handlerMethod = parameter.getHandlerMethod();
        ret = handlerMethod.getMethodAnnotation(DateTimeFormat.class);
        if (ret == null) {
          ret = handlerMethod.getDeclaringClassAnnotation(DateTimeFormat.class);
        }
      }

      parameter.setAttribute(FORMAT_ANNOTATION_KEY, ret == null ? EmptyObject.INSTANCE : ret);
      return ret;
    }
    else if (attribute == EmptyObject.INSTANCE) {
      return null;
    }
    return (DateTimeFormat) attribute;
  }

}
