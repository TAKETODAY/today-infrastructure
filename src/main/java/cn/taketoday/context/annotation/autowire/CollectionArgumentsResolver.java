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

package cn.taketoday.context.annotation.autowire;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;

import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2020/10/11 21:54
 * @since 3.0
 */
public class CollectionArgumentsResolver
        extends NonNullBeanFactoryStrategy implements ArgumentsResolvingStrategy {

  @Override
  protected boolean supportsInternal(Parameter parameter, ArgumentsResolvingContext context) {
    return Collection.class.isAssignableFrom(parameter.getType());
  }

  @Override
  protected Object resolveInternal(
          Parameter parameter, BeanFactory beanFactory, ArgumentsResolvingContext resolvingContext) {
    final ResolvableType parameterType = ResolvableType.fromParameter(parameter);
    if (parameterType.hasGenerics()) {
      final ResolvableType type = parameterType.asCollection().getGeneric(0);
      final Map<String, ?> beans = beanFactory.getBeansOfType(type.toClass());
      final Collection<Object> objects = CollectionUtils.createCollection(parameter.getType(), beans.size());
      objects.addAll(beans.values());
      return objects;
    }
    throw new ConfigurationException("Not Support " + parameter);
  }
}
