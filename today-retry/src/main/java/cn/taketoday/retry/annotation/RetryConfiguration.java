/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry.annotation;

import org.aopalliance.aop.Advice;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractPointcutAdvisor;
import cn.taketoday.aop.support.ComposablePointcut;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.aop.support.annotation.AnnotationClassFilter;
import cn.taketoday.aop.support.annotation.AnnotationMethodMatcher;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.stereotype.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.backoff.Sleeper;
import cn.taketoday.retry.interceptor.MethodArgumentsKeyGenerator;
import cn.taketoday.retry.interceptor.NewMethodArgumentsIdentifier;
import cn.taketoday.retry.policy.RetryContextCache;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Basic configuration for <code>@Retryable</code> processing. For stateful retry, if
 * there is a unique bean elsewhere in the context of type {@link RetryContextCache},
 * {@link MethodArgumentsKeyGenerator} or {@link NewMethodArgumentsIdentifier} it will be
 * used by the corresponding retry interceptor (otherwise sensible defaults are adopted).
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Markus Heiden
 * @since 4.0
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class RetryConfiguration extends AbstractPointcutAdvisor
        implements IntroductionAdvisor, BeanFactoryAware, InitializingBean {
  @Serial
  private static final long serialVersionUID = 1L;

  private Advice advice;

  private Pointcut pointcut;

  private RetryContextCache retryContextCache;

  private List<RetryListener> retryListeners;

  private MethodArgumentsKeyGenerator methodArgumentsKeyGenerator;

  private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;

  private Sleeper sleeper;

  private BeanFactory beanFactory;

  @Override
  public void afterPropertiesSet() {
    this.sleeper = findBean(Sleeper.class);
    this.retryListeners = findBeans(RetryListener.class);
    this.retryContextCache = findBean(RetryContextCache.class);
    this.methodArgumentsKeyGenerator = findBean(MethodArgumentsKeyGenerator.class);
    this.newMethodArgumentsIdentifier = findBean(NewMethodArgumentsIdentifier.class);
    var retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
    retryableAnnotationTypes.add(Retryable.class);
    this.pointcut = buildPointcut(retryableAnnotationTypes);
    this.advice = buildAdvice();
    if (this.advice instanceof BeanFactoryAware) {
      ((BeanFactoryAware) this.advice).setBeanFactory(this.beanFactory);
    }
  }

  @Nullable
  private <T> List<T> findBeans(Class<? extends T> type) {
    if (beanFactory.getBeanNamesForType(type).size() > 0) {
      ArrayList<T> list = new ArrayList<>(beanFactory.getBeansOfType(type, false, false).values());
      OrderComparator.sort(list);
      return list;
    }
    return null;
  }

  @Nullable
  private <T> T findBean(Class<? extends T> type) {
    if (beanFactory.getBeanNamesForType(type, false, false).size() == 1) {
      return beanFactory.getBean(type);
    }
    return null;
  }

  /**
   * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public ClassFilter getClassFilter() {
    return this.pointcut.getClassFilter();
  }

  @Override
  public Class<?>[] getInterfaces() {
    return new Class[] { cn.taketoday.retry.interceptor.Retryable.class };
  }

  @Override
  public void validateInterfaces() throws IllegalArgumentException {
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  protected Advice buildAdvice() {
    AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();
    if (this.retryContextCache != null) {
      interceptor.setRetryContextCache(this.retryContextCache);
    }
    if (this.retryListeners != null) {
      interceptor.setListeners(this.retryListeners);
    }
    if (this.methodArgumentsKeyGenerator != null) {
      interceptor.setKeyGenerator(this.methodArgumentsKeyGenerator);
    }
    if (this.newMethodArgumentsIdentifier != null) {
      interceptor.setNewItemIdentifier(this.newMethodArgumentsIdentifier);
    }
    if (this.sleeper != null) {
      interceptor.setSleeper(this.sleeper);
    }
    return interceptor;
  }

  /**
   * Calculate a pointcut for the given retry annotation types, if any.
   *
   * @param retryAnnotationTypes the retry annotation types to introspect
   * @return the applicable Pointcut object, or {@code null} if none
   */
  protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
    ComposablePointcut result = null;
    for (Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
      Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
      if (result == null) {
        result = new ComposablePointcut(filter);
      }
      else {
        result.union(filter);
      }
    }
    return result;
  }

  private static final class AnnotationClassOrMethodPointcut extends StaticMethodMatcherPointcut {
    private final MethodMatcher methodResolver;

    AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
      this.methodResolver = new AnnotationMethodMatcher(annotationType);
      setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return getClassFilter().matches(targetClass) || this.methodResolver.matches(method, targetClass);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof AnnotationClassOrMethodPointcut otherAdvisor)) {
        return false;
      }
      return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver);
    }

  }

  private static final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {

    private final AnnotationMethodsResolver methodResolver;

    AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
      super(annotationType, true);
      this.methodResolver = new AnnotationMethodsResolver(annotationType);
    }

    @Override
    public boolean matches(Class<?> clazz) {
      return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
    }

  }

  private record AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {

    public boolean hasAnnotatedMethods(Class<?> clazz) {
      final AtomicBoolean found = new AtomicBoolean(false);
      ReflectionUtils.doWithMethods(clazz, method -> {
        if (found.get()) {
          return;
        }
        Annotation annotation = AnnotationUtils.findAnnotation(method,
                AnnotationMethodsResolver.this.annotationType);
        if (annotation != null) {
          found.set(true);
        }
      });
      return found.get();
    }

  }

}
