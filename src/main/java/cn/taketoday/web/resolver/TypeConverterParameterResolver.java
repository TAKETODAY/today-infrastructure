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

import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-13 11:21
 */
public abstract class TypeConverterParameterResolver
        extends OrderedAbstractParameterResolver implements ParameterResolver {

  protected TypeConverterParameterResolver() {
    this(HIGHEST_PRECEDENCE);
  }

  protected TypeConverterParameterResolver(int order) {
    setOrder(order);
  }

  @Override
  public abstract boolean supports(MethodParameter parameter);

  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) {
    return context.parameter(parameter.getName());
  }

  @Override
  protected Object transformValue(RequestContext context, MethodParameter parameter, Object original) {
    return ConvertUtils.convert(original, resolveTargetClass(parameter));
  }

  protected Class<?> resolveTargetClass(final MethodParameter parameter) {
    return parameter.getParameterClass();
  }

}
