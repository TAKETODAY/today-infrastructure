/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * @author TODAY
 * @date 2020/10/11 21:54
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
    final Type[] genericityClass = ClassUtils.getGenericityClass(parameter);
    if (ObjectUtils.isNotEmpty(genericityClass)) {
      final Type type = genericityClass[0];
      final List<?> beans = beanFactory.getBeans((Class<?>) type);

      final Class<?> parameterType = parameter.getType();

      if (Collection.class == parameterType || List.class == parameterType) {
        return beans;
      }

      // Set<?>
      if (Set.class == parameterType) {
        return new HashSet<>(beans);
      }
      Collection ret = (Collection) ClassUtils.newInstance(parameterType, beanFactory);
      ret.addAll(beans);
      return ret;
    }
    throw new ConfigurationException("Not Support " + parameter);
  }
}
