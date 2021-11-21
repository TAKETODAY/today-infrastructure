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

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.Map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/17 16:03
 */
public class ArrayBeanDependencyResolver implements DependencyResolvingStrategy {

  @Override
  public void resolveDependency(InjectionPoint injectionPoint, DependencyResolvingContext context) {
    Class<?> type = injectionPoint.getDependencyType();
    Class<?> componentType = type.getComponentType();
    BeanFactory beanFactory = context.getBeanFactory();

    if (beanFactory != null && type.isArray() && !ClassUtils.isSimpleType(componentType)) {
      Map<String, ?> beans = beanFactory.getBeansOfType(componentType);
      if (CollectionUtils.isEmpty(beans)) {
        context.setDependency(Array.newInstance(componentType, 0));
      }
      else {
        context.setDependency(beans.values().toArray());
      }
    }
  }

}
