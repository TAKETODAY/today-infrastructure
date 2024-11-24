/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.scheduling.annotation;

import org.aopalliance.aop.Advice;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import infra.aop.Pointcut;
import infra.aop.interceptor.AsyncUncaughtExceptionHandler;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.aop.support.ComposablePointcut;
import infra.aop.support.annotation.AnnotationMatchingPointcut;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.function.SingletonSupplier;

/**
 * Advisor that activates asynchronous method execution through the {@link Async}
 * annotation. This annotation can be used at the method and type level in
 * implementation classes as well as in service interfaces.
 *
 * <p>This advisor detects the EJB 3.1 {@code jakarta.ejb.Asynchronous}
 * annotation as well, treating it exactly like  own {@code Async}.
 * Furthermore, a custom async annotation type may get specified through the
 * {@link #setAsyncAnnotationType "asyncAnnotationType"} property.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Async
 * @see AnnotationAsyncExecutionInterceptor
 * @since 4.0
 */
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Advice advice;

  private Pointcut pointcut;

  /**
   * Create a new {@code AsyncAnnotationAdvisor} for bean-style configuration.
   */
  public AsyncAnnotationAdvisor() {
    this(null, (Supplier<AsyncUncaughtExceptionHandler>) null);
  }

  /**
   * Create a new {@code AsyncAnnotationAdvisor} for the given task executor.
   *
   * @param executor the task executor to use for asynchronous methods
   * (can be {@code null} to trigger default executor resolution)
   * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
   * handle unexpected exception thrown by asynchronous method executions
   * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
   */
  public AsyncAnnotationAdvisor(@Nullable Executor executor,
          @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {
    this(SingletonSupplier.ofNullable(executor), SingletonSupplier.ofNullable(exceptionHandler));
  }

  /**
   * Create a new {@code AsyncAnnotationAdvisor} for the given task executor.
   *
   * @param executor the task executor to use for asynchronous methods
   * (can be {@code null} to trigger default executor resolution)
   * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
   * handle unexpected exception thrown by asynchronous method executions
   * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
   */
  public AsyncAnnotationAdvisor(@Nullable Supplier<Executor> executor,
          @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

    var asyncAnnoTypes = new LinkedHashSet<Class<? extends Annotation>>(2);
    asyncAnnoTypes.add(Async.class);

    ClassLoader classLoader = AsyncAnnotationAdvisor.class.getClassLoader();
    try {
      asyncAnnoTypes.add(ClassUtils.forName("jakarta.ejb.Asynchronous", classLoader));
    }
    catch (ClassNotFoundException ex) {
      // If EJB API not present, simply ignore.
    }
    try {
      asyncAnnoTypes.add(ClassUtils.forName("jakarta.enterprise.concurrent.Asynchronous", classLoader));
    }
    catch (ClassNotFoundException ex) {
      // If Jakarta Concurrent API not present, simply ignore.
    }

    this.advice = buildAdvice(executor, exceptionHandler);
    this.pointcut = buildPointcut(asyncAnnoTypes);
  }

  /**
   * Set the 'async' annotation type.
   * <p>The default async annotation type is the {@link Async} annotation, as well
   * as the EJB 3.1 {@code jakarta.ejb.Asynchronous} annotation (if present).
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation type to indicate that a method is to
   * be executed asynchronously.
   *
   * @param asyncAnnotationType the desired annotation type
   */
  public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
    Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' is required");
    this.pointcut = buildPointcut(Set.of(asyncAnnotationType));
  }

  /**
   * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (advice instanceof BeanFactoryAware aware) {
      aware.setBeanFactory(beanFactory);
    }
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

  protected Advice buildAdvice(@Nullable Supplier<Executor> executor,
          @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {
    var interceptor = new AnnotationAsyncExecutionInterceptor(null);
    interceptor.configure(executor, exceptionHandler);
    return interceptor;
  }

  /**
   * Calculate a pointcut for the given async annotation types, if any.
   *
   * @param asyncAnnotationTypes the async annotation types to introspect
   * @return the applicable Pointcut object, or {@code null} if none
   */
  protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
    ComposablePointcut result = null;
    for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
      var cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
      var mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
      if (result == null) {
        result = new ComposablePointcut(cpc);
      }
      else {
        result.union(cpc);
      }
      result = result.union(mpc);
    }
    return result != null ? result : Pointcut.TRUE;
  }

}
