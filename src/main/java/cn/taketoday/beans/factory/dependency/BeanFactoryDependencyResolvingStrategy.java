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

package cn.taketoday.beans.factory.dependency;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/27 22:18
 */
public class BeanFactoryDependencyResolvingStrategy
        extends AnnotationDependencyResolvingStrategy {
  protected static final Logger log = LoggerFactory.getLogger(BeanFactoryDependencyResolvingStrategy.class);

  @SuppressWarnings("rawtypes")
  private static final Class[] supportedAnnotations;

  static {
    LinkedHashSet<Class<? extends Annotation>> injectableAnnotations = new LinkedHashSet<>();
    injectableAnnotations.add(Autowired.class);
    ClassLoader classLoader = BeanFactoryDependencyResolvingStrategy.class.getClassLoader();
    // @formatter:off
    try {
      // Resource ?
      injectableAnnotations.add(ClassUtils.forName("jakarta.annotation.Resource", classLoader));
    }
    catch (Exception ignored) { }
    try {
      injectableAnnotations.add(
              ClassUtils.forName("jakarta.inject.Inject", classLoader));
      log.debug("'jakarta.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // jakarta.inject API not available - simply skip.
    }
    try {
      injectableAnnotations.add(
              ClassUtils.forName("javax.inject.Inject", classLoader));
      log.debug("'javax.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // javax.inject API not available - simply skip.
    }
    supportedAnnotations = injectableAnnotations.toArray(new Class[0]);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Class<? extends Annotation>[] getSupportedAnnotations() {
    return supportedAnnotations;
  }

  @Override
  public void resolveDependency(DependencyDescriptor descriptor, DependencyResolvingContext context) {
    BeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory instanceof AutowireCapableBeanFactory autowireCapable) {
      String beanName = context.getBeanName();
      Set<String> dependentBeans = null;
      if (beanName != null) {
        dependentBeans = context.dependentBeans();
      }
      try {
        Object dependency = autowireCapable.resolveDependency(descriptor, beanName, dependentBeans);
        context.setDependencyResolved(dependency);
      }
      catch (BeansException ex) {
        throw new UnsatisfiedDependencyException(null, beanName, descriptor, ex);
      }
    }
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

}
