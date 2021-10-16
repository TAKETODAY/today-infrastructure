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

package cn.taketoday.beans.factory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;

/**
 * @author TODAY 2021/10/16 22:23
 * @since 4.0
 */
final class ResolvableTypeObjectSupplier<T>
        extends AbstractResolvableTypeObjectSupplier<T> {

  private final AbstractBeanFactory beanFactory;

  ResolvableTypeObjectSupplier(AbstractBeanFactory beanFactory, ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    super(requiredType, includeNoneRegistered, includeNonSingletons);
    this.beanFactory = beanFactory;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected T getIfAvailable(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    ArrayList list = new ArrayList<>();

    for (Map.Entry<String, BeanDefinition> entry : beanFactory.getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (includeNonSingletons || def.isSingleton()) {
        if (requiredType == null || def.isAssignableTo(requiredType)) {
          list.add(def);
        }
      }
    }

    if (list.isEmpty() && includeNoneRegistered) {
      synchronized(beanFactory.getSingletons()) {
        for (Map.Entry<String, Object> entry : beanFactory.getSingletons().entrySet()) {
          Object bean = entry.getValue();
          if (AbstractBeanFactory.isInstance(requiredType, bean)) {
            list.add(bean);
          }
        }
        if (list.isEmpty()) {
          return null; // not found
        }
        AnnotationAwareOrderComparator.sort(list);
        return (T) list.get(0);
      }
    }
    else {
      BeanDefinition primary = beanFactory.getPrimaryBeanDefinition(list);
      return (T) beanFactory.getBean(primary);
    }
  }

  private Map<String, T> getBeansOfType() {
    return beanFactory.getBeansOfType(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public Stream<T> stream() {
    Map<String, T> beansOfType = getBeansOfType();
    return beansOfType.values().stream();
  }

  @Override
  public Stream<T> orderedStream() {
    Map<String, T> beansOfType = getBeansOfType();
    ArrayList<T> beans = new ArrayList<>(beansOfType.values());
    AnnotationAwareOrderComparator.sort(beans);
    return beans.stream();
  }

  @Override
  public Iterator<T> iterator() {
    Map<String, T> beansOfType = getBeansOfType();
    return beansOfType.values().iterator();
  }

}
