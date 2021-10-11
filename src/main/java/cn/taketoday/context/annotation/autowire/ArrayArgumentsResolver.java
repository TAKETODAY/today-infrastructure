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

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.Map;

import cn.taketoday.beans.ArgumentsResolvingContext;
import cn.taketoday.beans.ArgumentsResolvingStrategy;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/2/19 23:16
 */
public class ArrayArgumentsResolver
        extends NonNullBeanFactoryStrategy implements ArgumentsResolvingStrategy {

  @Override
  protected boolean supportsInternal(
          Parameter parameter, @NonNull ArgumentsResolvingContext context) {
    final Class<?> type = parameter.getType();
    return type.isArray() && !ClassUtils.isSimpleType(type.getComponentType());
  }

  @Override
  protected Object resolveInternal(
          Parameter parameter, BeanFactory beanFactory, ArgumentsResolvingContext resolvingContext) {
    final Class<?> parameterType = parameter.getType().getComponentType();
    final Map<String, ?> beans = beanFactory.getBeansOfType(parameterType);
    if (CollectionUtils.isEmpty(beans)) {
      return Array.newInstance(parameterType, 0);
    }
    return beans.values().toArray();
  }

}

