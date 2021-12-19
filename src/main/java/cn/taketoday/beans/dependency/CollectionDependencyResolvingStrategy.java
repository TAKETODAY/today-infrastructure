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

package cn.taketoday.beans.dependency;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.DependencyResolvingFailedException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryAwareOrderSourceProvider;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/17 20:00</a>
 * @since 4.0
 */
public class CollectionDependencyResolvingStrategy
        extends InjectableDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  protected boolean supportsInternal(
          InjectionPoint injectionPoint, DependencyResolvingContext context) {
    return Collection.class.isAssignableFrom(injectionPoint.getDependencyType());
  }

  @Override
  protected void resolveInternal(
          InjectionPoint injectionPoint, BeanFactory beanFactory, DependencyResolvingContext context) {
    ResolvableType resolvableType = injectionPoint.getResolvableType();
    if (resolvableType.hasGenerics()) {
      ResolvableType type = resolvableType.asCollection().getGeneric(0);
      if (type == ResolvableType.NONE) {
        throw new DependencyResolvingFailedException(
                "cannot determine a exactly bean type from injection-point: " + injectionPoint);
      }
      Set<String> beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type);

      Map<String, Object> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.size());
      for (String beanName : beanNames) {
        Object beanInstance = beanFactory.getBean(beanName);
        if (beanInstance != null) {
          matchingBeans.put(beanName, beanInstance);
        }
      }

      Collection<Object> objects = CollectionUtils.createCollection(
              injectionPoint.getDependencyType(), matchingBeans.size());
      if (!matchingBeans.isEmpty()) {
        objects.addAll(matchingBeans.values());
      }

      if (objects instanceof List<Object> list) {
        // ordering
        sort(beanFactory, matchingBeans, list);
      }
      context.setDependency(objects);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  static void sort(BeanFactory beanFactory, Map matchingBeans, List<Object> list) {
    FactoryAwareOrderSourceProvider sourceProvider =
            new FactoryAwareOrderSourceProvider(beanFactory, matchingBeans);
    Comparator<Object> objectComparator = AnnotationAwareOrderComparator.INSTANCE
            .withSourceProvider(sourceProvider);
    list.sort(objectComparator);
  }

}
