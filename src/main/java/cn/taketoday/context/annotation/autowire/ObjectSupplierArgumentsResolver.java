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

package cn.taketoday.context.annotation.autowire;

import java.lang.reflect.Parameter;
import java.util.function.Supplier;

import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;

/**
 * for {@link ObjectSupplier} ArgumentsResolverStrategy
 *
 * @author TODAY 2021/3/6 12:06
 */
public class ObjectSupplierArgumentsResolver implements ArgumentsResolvingStrategy {

  @Nullable
  @Override
  public ObjectSupplier<?> resolveArgument(Parameter parameter, ArgumentsResolvingContext resolvingContext) {
    BeanFactory beanFactory = resolvingContext.getBeanFactory();
    if (beanFactory != null) {
      Class<?> type = parameter.getType();
      if (type == ObjectSupplier.class || type == Supplier.class) {
        ResolvableType parameterType = ResolvableType.fromParameter(parameter);
        if (parameterType.hasGenerics()) {
          ResolvableType generic = parameterType.as(Supplier.class).getGeneric(0);
          return beanFactory.getObjectSupplier(generic);
        }
        throw new UnsupportedOperationException(
                "Unsupported '" + parameter + "' In -> " + parameter.getDeclaringExecutable());
      }
    }
    return null; // next
  }

}
