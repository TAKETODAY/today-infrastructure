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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.DateTimeFormat;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ParameterConversionException;
import cn.taketoday.web.resolver.ParameterResolver;

/**
 * @author TODAY 2021/2/23 21:25
 */
public abstract class AbstractDateParameterResolver
        extends OrderedSupport implements ParameterResolver {

  private DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
    final String parameterValue = getParameterValue(context, parameter);

    if(StringUtils.isEmpty(parameterValue)) {
      return null;
    }

    DateTimeFormatter formatter = getFormatter(parameter);
    try {
      return resolveInternal(parameterValue, formatter);
    }
    catch (DateTimeParseException e) {
      throw new ParameterConversionException(parameter, parameterValue, e);
    }
  }

  protected Object resolveInternal(String parameterValue, DateTimeFormatter formatter) {
    return null;
  }

  protected String getParameterValue(RequestContext context, MethodParameter parameter) {
    return context.parameter(parameter.getName());
  }

  protected DateTimeFormatter getFormatter(MethodParameter parameter) {
    final DateTimeFormat dateTimeFormat = parameter.getAnnotation(DateTimeFormat.class);
    if (dateTimeFormat != null) {
      final String pattern = dateTimeFormat.value();
      if (StringUtils.isNotEmpty(pattern)) {
        return DateTimeFormatter.ofPattern(pattern);
      }
    }
    return defaultFormatter;
  }

  public void setDefaultFormatter(DateTimeFormatter defaultFormatter) {
    this.defaultFormatter = defaultFormatter;
  }

  public DateTimeFormatter getDefaultFormatter() {
    return defaultFormatter;
  }

}
