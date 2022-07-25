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

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * Bean post-processor that automatically applies asynchronous invocation
 * behavior to any bean that carries the {@link Async} annotation at class or
 * method-level by adding a corresponding {@link AsyncAnnotationAdvisor} to the
 * exposed proxy (either an existing AOP proxy or a newly generated proxy that
 * implements all of the target's interfaces).
 *
 * <p>The {@link TaskExecutor} responsible for the asynchronous execution may
 * be provided as well as the annotation type that indicates a method should be
 * invoked asynchronously. If no annotation type is specified, this post-
 * processor will detect both  {@link Async @Async} annotation as well
 * as the EJB 3.1 {@code jakarta.ejb.Asynchronous} annotation.
 *
 * <p>For methods having a {@code void} return type, any exception thrown
 * during the asynchronous method invocation cannot be accessed by the
 * caller. An {@link AsyncUncaughtExceptionHandler} can be specified to handle
 * these cases.
 *
 * <p>Note: The underlying async advisor applies before existing advisors by default,
 * in order to switch to async execution as early as possible in the invocation chain.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see Async
 * @see AsyncAnnotationAdvisor
 * @see #setBeforeExistingAdvisors
 * @see ScheduledAnnotationBeanPostProcessor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AsyncAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

  /**
   * The default name of the {@link TaskExecutor} bean to pick up: "taskExecutor".
   * <p>Note that the initial lookup happens by type; this is just the fallback
   * in case of multiple executor beans found in the context.
   *
   * @see AnnotationAsyncExecutionInterceptor#DEFAULT_TASK_EXECUTOR_BEAN_NAME
   */
  public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME =
          AnnotationAsyncExecutionInterceptor.DEFAULT_TASK_EXECUTOR_BEAN_NAME;

  @Nullable
  private Supplier<Executor> executor;

  @Nullable
  private Supplier<AsyncUncaughtExceptionHandler> exceptionHandler;

  @Nullable
  private Class<? extends Annotation> asyncAnnotationType;

  public AsyncAnnotationBeanPostProcessor() {
    setBeforeExistingAdvisors(true);
  }

  /**
   * Configure this post-processor with the given executor and exception handler suppliers,
   * applying the corresponding default if a supplier is not resolvable.
   */
  public void configure(
          @Nullable Supplier<Executor> executor,
          @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {
    this.executor = executor;
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Set the {@link Executor} to use when invoking methods asynchronously.
   * <p>If not specified, default executor resolution will apply: searching for a
   * unique {@link TaskExecutor} bean in the context, or for an {@link Executor}
   * bean named "taskExecutor" otherwise. If neither of the two is resolvable,
   * a local default executor will be created within the interceptor.
   *
   * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
   * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
   */
  public void setExecutor(Executor executor) {
    this.executor = SingletonSupplier.valueOf(executor);
  }

  /**
   * Set the {@link AsyncUncaughtExceptionHandler} to use to handle uncaught
   * exceptions thrown by asynchronous method executions.
   */
  public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
    this.exceptionHandler = SingletonSupplier.valueOf(exceptionHandler);
  }

  /**
   * Set the 'async' annotation type to be detected at either class or method
   * level. By default, both the {@link Async} annotation and the EJB 3.1
   * {@code jakarta.ejb.Asynchronous} annotation will be detected.
   * <p>This setter property exists so that developers can provide their own
   * (non-specific) annotation type to indicate that a method (or all
   * methods of a given class) should be invoked asynchronously.
   *
   * @param asyncAnnotationType the desired annotation type
   */
  public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
    Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
    this.asyncAnnotationType = asyncAnnotationType;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);

    AsyncAnnotationAdvisor advisor = new AsyncAnnotationAdvisor(this.executor, this.exceptionHandler);
    if (this.asyncAnnotationType != null) {
      advisor.setAsyncAnnotationType(this.asyncAnnotationType);
    }
    advisor.setBeanFactory(beanFactory);
    this.advisor = advisor;
  }

}
