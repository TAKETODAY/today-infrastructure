/**
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

import java.lang.reflect.Method;

import cn.taketoday.core.Assert;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.utils.GenericDescriptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-13 11:21
 */
public abstract class ConvertibleParameterResolver
        extends OrderedAbstractParameterResolver implements ParameterResolver {

  protected ConversionService conversionService = DefaultConversionService.getSharedInstance();

  protected ConvertibleParameterResolver() {
    this(HIGHEST_PRECEDENCE);
  }

  protected ConvertibleParameterResolver(int order) {
    setOrder(order);
  }

  @Override
  public abstract boolean supports(MethodParameter parameter);

  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) {
    return context.getParameter(parameter.getName());
  }

  @Override
  protected Object transformValue(
          final RequestContext context, final MethodParameter parameter, final Object original) {

    final Method method = parameter.getHandlerMethod().getMethod();
    final GenericDescriptor targetType = GenericDescriptor.ofParameter(method, parameter.getParameterIndex());

    return obtainConversionService().convert(original, targetType);
  }

  protected Class<?> resolveTargetClass(final MethodParameter parameter) {
    return parameter.getParameterClass();
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  protected ConversionService obtainConversionService() {
    final ConversionService conversionService = getConversionService();
    Assert.state(conversionService != null, "No ConversionService set");
    return conversionService;
  }

}
