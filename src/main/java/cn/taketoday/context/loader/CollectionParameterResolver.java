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
import java.util.Collection;
import java.util.Map;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ResolvableType;

/**
 * @author TODAY
 * 2020/10/11 21:54
 * @since 3.0
 */
public class CollectionParameterResolver
        extends OrderedSupport implements ExecutableParameterResolver {

  public CollectionParameterResolver() {
    this(Integer.MAX_VALUE);
  }

  public CollectionParameterResolver(int order) {
    super(order);
  }

  @Override
  public boolean supports(final Parameter parameter) {
    return Collection.class.isAssignableFrom(parameter.getType());
  }

  @Override
  public Object resolve(final Parameter parameter, final BeanFactory beanFactory) {
    final ResolvableType parameterType = ResolvableType.forParameter(parameter);
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
