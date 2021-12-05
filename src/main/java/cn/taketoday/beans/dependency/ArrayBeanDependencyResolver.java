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

import java.lang.reflect.Array;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/17 16:03
 */
public class ArrayBeanDependencyResolver
        extends InjectableDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  protected boolean supportsInternal(
          InjectionPoint injectionPoint, DependencyResolvingContext context) {
    Class<?> type = injectionPoint.getDependencyType();
    Class<?> componentType = type.getComponentType();
    return componentType != null && !ClassUtils.isSimpleType(componentType);
  }

  @Override
  protected void resolveInternal(
          InjectionPoint injectionPoint, BeanFactory beanFactory, DependencyResolvingContext context) {
    Class<?> componentType = injectionPoint.getDependencyType().getComponentType();
    List<?> beans = beanFactory.getBeans(componentType);
    if (CollectionUtils.isEmpty(beans)) {
      context.setDependency(Array.newInstance(componentType, 0));
    }
    else {
      Object array = Array.newInstance(componentType, beans.size());
      AnnotationAwareOrderComparator.sort(beans);
      context.setDependency(beans.toArray((Object[]) array));
    }
  }

}
