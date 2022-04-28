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

package cn.taketoday.web.bind.resolver.date;

import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.lang.NullValue;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY 2021/3/2 12:30
 * @since 3.0
 */
public abstract class AbstractDateParameterResolver implements ParameterResolvingStrategy {
  protected static final String FORMAT_ANNOTATION_KEY = AbstractDateParameterResolver.class.getName() + "-DateTimeFormat";

  @Override
  public abstract boolean supportsParameter(ResolvableMethodParameter parameter);

  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    final String parameterValue = getParameterValue(context, resolvable);
    if (StringUtils.isEmpty(parameterValue)) {
      return null;
    }
    return resolveInternal(parameterValue, resolvable);
  }

  protected Object resolveInternal(String parameterValue, ResolvableMethodParameter parameter) {
    return null;
  }

  protected String getParameterValue(RequestContext context, ResolvableMethodParameter parameter) {
    return context.getParameter(parameter.getName());
  }

  protected DateTimeFormat getAnnotation(ResolvableMethodParameter parameter) {
    final Object attribute = parameter.getAttribute(FORMAT_ANNOTATION_KEY);
    if (attribute == null) {
      DateTimeFormat ret = parameter.getParameterAnnotation(DateTimeFormat.class);
      if (ret == null) {
        ret = parameter.getParameter().getMethodAnnotation(DateTimeFormat.class);
        if (ret == null) {
          ret = parameter.getParameter().getContainingClass().getAnnotation(DateTimeFormat.class);
        }
      }

      parameter.setAttribute(FORMAT_ANNOTATION_KEY, ret == null ? NullValue.INSTANCE : ret);
      return ret;
    }
    else if (attribute == NullValue.INSTANCE) {
      return null;
    }
    return (DateTimeFormat) attribute;
  }

}
