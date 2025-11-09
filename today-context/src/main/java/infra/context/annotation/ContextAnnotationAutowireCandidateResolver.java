/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.aop.TargetSource;
import infra.aop.framework.ProxyFactory;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.AutowireCandidateResolver;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.MethodParameter;
import infra.core.annotation.AnnotationUtils;

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

  @Override
  @Nullable
  public Class<?> getLazyResolutionProxyClass(DependencyDescriptor descriptor, @Nullable String beanName) {
    return (isLazy(descriptor) ? (Class<?>) buildLazyResolutionProxy(descriptor, beanName, true) : null);
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

  protected Object buildLazyResolutionProxy(DependencyDescriptor descriptor, @Nullable String beanName) {
    return buildLazyResolutionProxy(descriptor, beanName, false);
  }

  private Object buildLazyResolutionProxy(DependencyDescriptor descriptor, @Nullable String beanName, boolean classOnly) {

    if (!(getBeanFactory() instanceof StandardBeanFactory stdF)) {
      throw new IllegalStateException("Lazy resolution only supported with StandardBeanFactory");
    }

    TargetSource ts = new LazyDependencyTargetSource(stdF, descriptor, beanName);

    ProxyFactory pf = new ProxyFactory();
    pf.setTargetSource(ts);
    Class<?> dependencyType = descriptor.getDependencyType();
    if (dependencyType.isInterface()) {
      pf.addInterface(dependencyType);
    }
    ClassLoader classLoader = stdF.getBeanClassLoader();
    return classOnly ? pf.getProxyClass(classLoader) : pf.getProxy(classLoader);
  }

  @SuppressWarnings("serial")
  private static class LazyDependencyTargetSource implements TargetSource, Serializable {

    private final StandardBeanFactory beanFactory;

    private final DependencyDescriptor descriptor;

    @Nullable
    private final String beanName;

    @Nullable
    private transient volatile Object cachedTarget;

    public LazyDependencyTargetSource(StandardBeanFactory beanFactory,
            DependencyDescriptor descriptor, @Nullable String beanName) {

      this.beanFactory = beanFactory;
      this.descriptor = descriptor;
      this.beanName = beanName;
    }

    @Override
    public Class<?> getTargetClass() {
      return this.descriptor.getDependencyType();
    }

    @Override
    @SuppressWarnings("NullAway")
    public Object getTarget() {
      Object cachedTarget = this.cachedTarget;
      if (cachedTarget != null) {
        return cachedTarget;
      }

      Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
      Object target = this.beanFactory.doResolveDependency(
              this.descriptor, this.beanName, autowiredBeanNames, null);

      if (target == null) {
        Class<?> type = getTargetClass();
        if (Map.class == type) {
          target = Collections.emptyMap();
        }
        else if (List.class == type) {
          target = Collections.emptyList();
        }
        else if (Set.class == type || Collection.class == type) {
          target = Collections.emptySet();
        }
        else {
          throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType(),
                  "Optional dependency not present for lazy injection point");
        }
      }
      else {
        if (target instanceof Map<?, ?> map && Map.class == getTargetClass()) {
          target = Collections.unmodifiableMap(map);
        }
        else if (target instanceof List<?> list && List.class == getTargetClass()) {
          target = Collections.unmodifiableList(list);
        }
        else if (target instanceof Set<?> set && Set.class == getTargetClass()) {
          target = Collections.unmodifiableSet(set);
        }
        else if (target instanceof Collection<?> coll && Collection.class == getTargetClass()) {
          target = Collections.unmodifiableCollection(coll);
        }
      }

      boolean cacheable = true;
      for (String autowiredBeanName : autowiredBeanNames) {
        if (!this.beanFactory.containsBean(autowiredBeanName)) {
          cacheable = false;
        }
        else {
          if (this.beanName != null) {
            this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
          }
          if (!this.beanFactory.isSingleton(autowiredBeanName)) {
            cacheable = false;
          }
        }
        if (cacheable) {
          this.cachedTarget = target;
        }
      }

      return target;
    }
  }

}
