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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Complete implementation of the {@link AutowireCandidateResolver} strategy
 * interface, providing support for qualifier annotations as well as for lazy resolution
 * driven by the {@link Lazy} annotation in the {@code context.annotation} package.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/22 22:11
 */
public class ContextAnnotationAutowireCandidateResolver extends QualifierAnnotationAutowireCandidateResolver {

  @Override
  @Nullable
  public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
    return isLazy(descriptor)
           ? buildLazyResolutionProxy(descriptor, beanName)
           : null;
  }

  protected boolean isLazy(DependencyDescriptor descriptor) {
    for (Annotation ann : descriptor.getAnnotations()) {
      Lazy lazy = AnnotationUtils.getAnnotation(ann, Lazy.class);
      if (lazy != null && lazy.value()) {
        return true;
      }
    }
    MethodParameter methodParam = descriptor.getMethodParameter();
    if (methodParam != null) {
      Method method = methodParam.getMethod();
      if (method == null || void.class == method.getReturnType()) {
        Lazy lazy = AnnotationUtils.getAnnotation(methodParam.getAnnotatedElement(), Lazy.class);
        return lazy != null && lazy.value();
      }
    }
    return false;
  }

  protected Object buildLazyResolutionProxy(
          final DependencyDescriptor descriptor, final @Nullable String beanName) {
    BeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory instanceof StandardBeanFactory,
            "BeanFactory needs to be a StandardBeanFactory");
    final StandardBeanFactory dlbf = (StandardBeanFactory) beanFactory;

    TargetSource ts = new TargetSource() {
      @Override
      public Class<?> getTargetClass() {
        return descriptor.getDependencyType();
      }

      @Override
      public boolean isStatic() {
        return false;
      }

      @Override
      public Object getTarget() {
        Set<String> autowiredBeanNames = beanName != null ? new LinkedHashSet<>(1) : null;
        Object target = dlbf.doResolveDependency(descriptor, beanName, autowiredBeanNames, null);
        if (target == null) {
          Class<?> type = getTargetClass();
          if (Map.class == type) {
            return Collections.emptyMap();
          }
          else if (List.class == type) {
            return Collections.emptyList();
          }
          else if (Set.class == type || Collection.class == type) {
            return Collections.emptySet();
          }
          throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
                  "Optional dependency not present for lazy injection point");
        }
        if (autowiredBeanNames != null) {
          for (String autowiredBeanName : autowiredBeanNames) {
            if (dlbf.containsBean(autowiredBeanName)) {
              dlbf.registerDependentBean(autowiredBeanName, beanName);
            }
          }
        }
        return target;
      }

      @Override
      public void releaseTarget(Object target) { }
    };

    ProxyFactory pf = new ProxyFactory();
    pf.setTargetSource(ts);
    Class<?> dependencyType = descriptor.getDependencyType();
    if (dependencyType.isInterface()) {
      pf.addInterface(dependencyType);
    }
    return pf.getProxy(dlbf.getBeanClassLoader());
  }

}
