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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.OrderSourceProvider;
import cn.taketoday.lang.Nullable;

/**
 * An {@link cn.taketoday.core.OrderSourceProvider} implementation
 * that is aware of the bean metadata of the instances to sort.
 * <p>Lookup for the method factory of an instance to sort, if any, and let the
 * comparator retrieve the {@link cn.taketoday.core.Order}
 * value defined on it. This essentially allows for the following construct:
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/19 17:26
 */
final class FactoryAwareOrderSourceProvider implements OrderSourceProvider {

  private final StandardBeanFactory beanFactory;
  private final IdentityHashMap<Object, String> instancesToBeanNames;

  public FactoryAwareOrderSourceProvider(StandardBeanFactory beanFactory, Map<String, ?> beans) {
    this.beanFactory = beanFactory;
    this.instancesToBeanNames = new IdentityHashMap<>();
    for (Map.Entry<String, ?> entry : beans.entrySet()) {
      instancesToBeanNames.put(entry.getValue(), entry.getKey());
    }
  }

  @Override
  @Nullable
  public Object getOrderSource(Object obj) {
    String beanName = this.instancesToBeanNames.get(obj);
    if (beanName == null || !beanFactory.containsBeanDefinition(beanName)) {
      return null;
    }
    RootBeanDefinition beanDefinition = beanFactory.getMergedLocalBeanDefinition(beanName);
    List<Object> sources = new ArrayList<>(2);
    Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
    if (factoryMethod != null) {
      sources.add(factoryMethod);
    }
    Class<?> targetType = beanDefinition.getTargetType();
    if (targetType != null && targetType != obj.getClass()) {
      sources.add(targetType);
    }
    return sources.toArray();
  }
}
