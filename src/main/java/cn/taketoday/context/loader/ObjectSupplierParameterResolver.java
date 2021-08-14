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

package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.utils.ResolvableType;

/**
 * for {@link ObjectSupplier} ExecutableParameterResolver
 *
 * @author TODAY 2021/3/6 12:06
 */
public class ObjectSupplierParameterResolver
        extends OrderedSupport implements ExecutableParameterResolver, Ordered {

  public ObjectSupplierParameterResolver() {
    this(Integer.MAX_VALUE);
  }

  public ObjectSupplierParameterResolver(int order) {
    super(order);
  }

  @Override
  public boolean supports(final Parameter parameter) {
    final Class<?> type = parameter.getType();
    return type == ObjectSupplier.class || type == Supplier.class;
  }

  @Override
  public ObjectSupplier<?> resolve(final Parameter parameter, final BeanFactory beanFactory) {
    final ResolvableType parameterType = ResolvableType.forParameter(parameter);
    if (parameterType.hasGenerics()) {
      final ResolvableType generic = parameterType.as(Supplier.class).getGeneric(0);
      return beanFactory.getBeanSupplier(generic.toClass());
    }
    throw new UnsupportedOperationException("Unsupported '" + parameter + "' In -> " + parameter.getDeclaringExecutable());
  }

}
