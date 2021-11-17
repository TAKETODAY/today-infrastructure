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
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Autowired;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 23:06</a>
 * @since 4.0
 */
public class AutowiredDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  public void resolveDependency(DependencyInjectionPoint injectionPoint, DependencyResolvingContext resolvingContext) {
    BeanFactory beanFactory = resolvingContext.getBeanFactory();
    if (beanFactory != null) {
      Class<?> dependencyType = injectionPoint.getDependencyType();
      MergedAnnotations annotations = injectionPoint.getAnnotations();
      MergedAnnotation<Autowired> autowired = annotations.get(Autowired.class); // @Autowired on parameter
      Object bean = resolveBean(autowired, dependencyType, beanFactory);
      if (bean == null) {
        if (injectionPoint.isRequired()) { // if it is required
          throw new NoSuchBeanDefinitionException(
                  "[" + injectionPoint + "] is required and there isn't a [" + dependencyType + "] bean", (Throwable) null);
        }
      }

      resolvingContext.setDependency(bean);
    }
  }

  protected Object resolveBean(
          MergedAnnotation<Autowired> autowired, Class<?> type, BeanFactory beanFactory) {
    if (autowired.isPresent()) {
      String name = autowired.getString(MergedAnnotation.VALUE);
      if (StringUtils.isNotEmpty(name)) {
        // use name and bean type to get bean
        return beanFactory.getBean(name, type);
      }
    }
    return beanFactory.getBean(type);
  }

}
