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

package cn.taketoday.scheduling.annotation;

import org.aopalliance.aop.Advice;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractPointcutAdvisor;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.ComposablePointcut;
import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.SingletonSupplier;

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
  public AsyncAnnotationAdvisor(
          @Nullable Executor executor, @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {

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
  public AsyncAnnotationAdvisor(
          @Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

    Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<>(2);
    asyncAnnotationTypes.add(Async.class);
    try {
      asyncAnnotationTypes.add(
              ClassUtils.forName("jakarta.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
    }
    catch (ClassNotFoundException ex) {
      // If EJB 3.1 API not present, simply ignore.
    }
    this.advice = buildAdvice(executor, exceptionHandler);
    this.pointcut = buildPointcut(asyncAnnotationTypes);
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
    Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
    Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<>();
    asyncAnnotationTypes.add(asyncAnnotationType);
    this.pointcut = buildPointcut(asyncAnnotationTypes);
  }

  /**
   * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (this.advice instanceof BeanFactoryAware) {
      ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
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

  protected Advice buildAdvice(
          @Nullable Supplier<Executor> executor,
          @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

    AnnotationAsyncExecutionInterceptor interceptor = new AnnotationAsyncExecutionInterceptor(null);
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
      Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
      Pointcut mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
      if (result == null) {
        result = new ComposablePointcut(cpc);
      }
      else {
        result.union(cpc);
      }
      result = result.union(mpc);
    }
    return (result != null ? result : Pointcut.TRUE);
  }

}
