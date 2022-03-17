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

package cn.taketoday.scheduling.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.scheduling.annotation.Async;

/**
 * Aspect to route methods based on Framework's {@link Async} annotation.
 *
 * <p>This aspect routes methods marked with the {@link Async} annotation as well as methods
 * in classes marked with the same. Any method expected to be routed asynchronously must
 * return either {@code void}, {@link Future}, or a subtype of {@link Future} (in particular,
 * Framework's {@link cn.taketoday.util.concurrent.ListenableFuture}). This aspect,
 * therefore, will produce a compile-time error for methods that violate this constraint
 * on the return type. If, however, a class marked with {@code @Async} contains a method
 * that violates this constraint, it produces only a warning.
 *
 * <p>This aspect needs to be injected with an implementation of a task-oriented
 * {@link java.util.concurrent.Executor} to activate it for a specific thread pool,
 * or with a {@link cn.taketoday.beans.factory.BeanFactory} for default
 * executor lookup. Otherwise it will simply delegate all calls synchronously.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @since 4.0
 * @see #setExecutor
 * @see #setBeanFactory
 * @see #getDefaultExecutor
 */
public aspect AnnotationAsyncExecutionAspect extends AbstractAsyncExecutionAspect {

  private pointcut asyncMarkedMethod(): execution(@Async (void || Future+) *(..));

  private pointcut asyncTypeMarkedMethod(): execution((void || Future+) (@Async *).*(..));

  public pointcut asyncMethod(): asyncMarkedMethod() || asyncTypeMarkedMethod();


  /**
   * This implementation inspects the given method and its declaring class for the
   * {@code @Async} annotation, returning the qualifier value expressed by {@link Async#value()}.
   * If {@code @Async} is specified at both the method and class level, the method's
   * {@code #value} takes precedence (even if empty string, indicating that the default
   * executor should be used preferentially).
   * @return the qualifier if specified, otherwise empty string indicating that the
   * {@linkplain #setExecutor default executor} should be used
   * @see #determineAsyncExecutor(Method)
   */
  @Override
  protected String getExecutorQualifier(Method method) {
    // Maintainer's note: changes made here should also be made in
    // AnnotationAsyncExecutionInterceptor#getExecutorQualifier
    Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
    if (async == null) {
      async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
    }
    return (async != null ? async.value() : null);
  }


  declare error:
          execution(@Async !(void || Future+) *(..)):
          "Only methods that return void or Future may have an @Async annotation";

  declare warning:
          execution(!(void || Future+) (@Async *).*(..)):
          "Methods in a class marked with @Async that do not return void or Future will be routed synchronously";

}
