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

package infra.retry.annotation;

import org.aopalliance.aop.Advice;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.aop.ClassFilter;
import infra.aop.IntroductionAdvisor;
import infra.aop.MethodMatcher;
import infra.aop.Pointcut;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.aop.support.ComposablePointcut;
import infra.aop.support.StaticMethodMatcherPointcut;
import infra.aop.support.annotation.AnnotationClassFilter;
import infra.aop.support.annotation.AnnotationMethodMatcher;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.ImportAware;
import infra.context.annotation.Role;
import infra.core.OrderComparator;
import infra.core.annotation.AnnotationAttributes;
import infra.core.annotation.AnnotationUtils;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.retry.RetryListener;
import infra.retry.backoff.Sleeper;
import infra.retry.interceptor.MethodArgumentsKeyGenerator;
import infra.retry.interceptor.NewMethodArgumentsIdentifier;
import infra.retry.policy.RetryContextCache;
import infra.stereotype.Component;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;

/**
 * Basic configuration for <code>@Retryable</code> processing. For stateful retry, if
 * there is a unique bean elsewhere in the context of type {@link RetryContextCache},
 * {@link MethodArgumentsKeyGenerator} or {@link NewMethodArgumentsIdentifier} it will be
 * used by the corresponding retry interceptor (otherwise sensible defaults are adopted).
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Markus Heiden
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class RetryConfiguration extends AbstractPointcutAdvisor implements IntroductionAdvisor,
        BeanFactoryAware, InitializingBean, SmartInitializingSingleton, ImportAware {

  @Serial
  private static final long serialVersionUID = 1L;

  private AnnotationAwareRetryOperationsInterceptor advice;

  private Pointcut pointcut;

  private RetryContextCache retryContextCache;

  private MethodArgumentsKeyGenerator methodArgumentsKeyGenerator;

  private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;

  private Sleeper sleeper;

  private BeanFactory beanFactory;

  @Nullable
  protected AnnotationAttributes enableRetry;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableRetry = AnnotationAttributes
            .fromMap(importMetadata.getAnnotationAttributes(EnableRetry.class));
  }

  @Override
  public void afterPropertiesSet() {
    this.sleeper = findBean(Sleeper.class);
    this.retryContextCache = findBean(RetryContextCache.class);
    this.methodArgumentsKeyGenerator = findBean(MethodArgumentsKeyGenerator.class);
    this.newMethodArgumentsIdentifier = findBean(NewMethodArgumentsIdentifier.class);
    var retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
    retryableAnnotationTypes.add(Retryable.class);
    this.pointcut = buildPointcut(retryableAnnotationTypes);
    this.advice = buildAdvice();
    if (enableRetry != null) {
      setOrder(enableRetry.getNumber("order"));
    }
  }

  @Nullable
  private <T> List<T> findBeans(Class<? extends T> type) {
    if (!beanFactory.getBeanNamesForType(type).isEmpty()) {
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
    return new Class[] { infra.retry.interceptor.Retryable.class };
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

  @Override
  public void afterSingletonsInstantiated() {
    List<RetryListener> retryListeners = findBeans(RetryListener.class);
    if (retryListeners != null) {
      advice.setListeners(retryListeners);
    }
  }

  protected AnnotationAwareRetryOperationsInterceptor buildAdvice() {
    AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();
    if (this.retryContextCache != null) {
      interceptor.setRetryContextCache(this.retryContextCache);
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
    interceptor.setBeanFactory(this.beanFactory);
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

    @Override
    public int hashCode() {
      return Objects.hash(methodResolver);
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
