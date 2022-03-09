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

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import cn.taketoday.aop.interceptor.AsyncExecutionInterceptor;
import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Nullable;

/**
 * Specialization of {@link AsyncExecutionInterceptor} that
 * delegates method execution to an {@code Executor} based
 * on the {@link Async} annotation. Supports detecting
 * qualifier metadata via {@code @Async} at the method or
 * declaring class level. See {@link #getExecutorQualifier(Method)} for details.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @see cn.taketoday.scheduling.annotation.Async
 * @see cn.taketoday.scheduling.annotation.AsyncAnnotationAdvisor
 * @since 4.0
 */
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

  /**
   * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor
   * and a simple {@link AsyncUncaughtExceptionHandler}.
   *
   * @param defaultExecutor the executor to be used by default if no more specific
   * executor has been qualified at the method level using {@link Async#value()};
   * Now a local executor for this interceptor will be built otherwise
   */
  public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
    super(defaultExecutor);
  }

  /**
   * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor.
   *
   * @param defaultExecutor the executor to be used by default if no more specific
   * executor has been qualified at the method level using {@link Async#value()};
   * a local executor for this interceptor will be built otherwise
   * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
   * handle exceptions thrown by asynchronous method executions with {@code void}
   * return type
   */
  public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
    super(defaultExecutor, exceptionHandler);
  }

  /**
   * Return the qualifier or bean name of the executor to be used when executing the
   * given method, specified via {@link Async#value} at the method or declaring
   * class level. If {@code @Async} is specified at both the method and class level, the
   * method's {@code #value} takes precedence (even if empty string, indicating that
   * the default executor should be used preferentially).
   *
   * @param method the method to inspect for executor qualifier metadata
   * @return the qualifier if specified, otherwise empty string indicating that the
   * {@linkplain #setExecutor(Executor) default executor} should be used
   * @see #determineAsyncExecutor(Method)
   */
  @Override
  @Nullable
  protected String getExecutorQualifier(Method method) {
    // Maintainer's note: changes made here should also be made in
    // AnnotationAsyncExecutionAspect#getExecutorQualifier
    Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
    if (async == null) {
      async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
    }
    return async != null ? async.value() : null;
  }

}
