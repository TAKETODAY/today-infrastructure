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

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import cn.taketoday.aop.interceptor.AsyncExecutionAspectSupport;
import cn.taketoday.core.task.AsyncTaskExecutor;

/**
 * Abstract aspect that routes selected methods asynchronously.
 *
 * <p>This aspect needs to be injected with an implementation of a task-oriented
 * {@link java.util.concurrent.Executor} to activate it for a specific thread pool,
 * or with a {@link cn.taketoday.beans.factory.BeanFactory} for default
 * executor lookup. Otherwise it will simply delegate all calls synchronously.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 4.0
 * @see #setExecutor
 * @see #setBeanFactory
 * @see #getDefaultExecutor
 */
public abstract aspect AbstractAsyncExecutionAspect extends AsyncExecutionAspectSupport {

  /**
   * Create an {@code AnnotationAsyncExecutionAspect} with a {@code null}
   * default executor, which should instead be set via {@code #aspectOf} and
   * {@link #setExecutor}. The same applies for {@link #setExceptionHandler}.
   */
  public AbstractAsyncExecutionAspect() {
    super(null);
  }

  /**
   * Apply around advice to methods matching the {@link #asyncMethod()} pointcut,
   * submit the actual calling of the method to the correct task executor and return
   * immediately to the caller.
   * @return {@link Future} if the original method returns {@code Future};
   * {@code null} otherwise
   */
  @SuppressAjWarnings("adviceDidNotMatch")
  Object around(): asyncMethod() {
    final MethodSignature methodSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();

    AsyncTaskExecutor executor = determineAsyncExecutor(methodSignature.getMethod());
    if (executor == null) {
      return proceed();
    }

    Callable<Object> task = new Callable<Object>() {
      public Object call() throws Exception {
        try {
          Object result = proceed();
          if (result instanceof Future) {
            return ((Future<?>) result).get();
          }
        }
        catch (Throwable ex) {
          handleError(ex, methodSignature.getMethod(), thisJoinPoint.getArgs());
        }
        return null;
      }
    };

    return doSubmit(task, executor, methodSignature.getReturnType());
  }

  /**
   * Return the set of joinpoints at which async advice should be applied.
   */
  public abstract pointcut asyncMethod();

}
