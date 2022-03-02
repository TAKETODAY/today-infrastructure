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

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * ConversionService convert original to target
 *
 * @author TODAY 2019-07-13 11:21
 * @see ConversionService
 * @see #transformValue(RequestContext, ResolvableMethodParameter, Object)
 */
public abstract class ConversionServiceParameterResolver
        extends AbstractParameterResolver implements ParameterResolvingStrategy, ConversionServiceAware {

  protected ConversionService conversionService = DefaultConversionService.getSharedInstance();

  @Override
  public abstract boolean supportsParameter(ResolvableMethodParameter resolvable)  ;

  @Override
  protected Object resolveInternal(final RequestContext context, final ResolvableMethodParameter parameter) {
    return context.getParameter(parameter.getName());
  }

  /**
   * use conversion-service to convert original to target
   */
  @Override
  protected Object transformValue(
          final RequestContext context, final ResolvableMethodParameter parameter, final Object original) {
    TypeDescriptor targetType = parameter.getTypeDescriptor();
    return conversionService.convert(original, targetType);
  }

  @Override
  public void setConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService must not be null");
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

}
