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

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-13 12:58
 */
public class ConverterParameterResolver
        extends AbstractParameterResolver implements ParameterResolvingStrategy {
  private final SupportsFunction supports;
  private final Converter<String, Object> converter;

  public ConverterParameterResolver(Class<?> targetType, Converter<String, Object> converter) {
    this(new TargetSupportsFunction(targetType), converter);
  }

  public ConverterParameterResolver(SupportsFunction supports, Converter<String, Object> converter) {
    this.supports = supports;
    this.converter = converter;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return supports.supports(parameter);
  }

  @Override
  protected Object resolveInternal(RequestContext context, MethodParameter parameter) throws Throwable {
    return context.getParameter(parameter.getName());
  }

  @Override
  protected Object transformValue(RequestContext context, MethodParameter parameter, Object original) {
    return converter.convert((String) original);
  }

  @Override
  protected Object fromDefaultValue(RequestContext context, String defaultValue) {
    return converter.convert(defaultValue);
  }

  public static ConverterParameterResolver from(
          Class<?> targetType, Converter<String, Object> converter) {
    return new ConverterParameterResolver(targetType, converter);
  }

  public static ConverterParameterResolver from(
          SupportsFunction supports, Converter<String, Object> converter) {
    return new ConverterParameterResolver(supports, converter);
  }

}
