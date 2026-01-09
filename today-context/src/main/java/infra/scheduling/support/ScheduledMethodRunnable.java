/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import infra.scheduling.SchedulingAwareRunnable;
import infra.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import infra.util.ReflectionUtils;

/**
 * Variant of {@link MethodInvokingRunnable} meant to be used for processing
 * of no-arg scheduled methods. Propagates user exceptions to the caller,
 * assuming that an error strategy for Runnables is in place.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScheduledAnnotationBeanPostProcessor
 * @since 4.0
 */
public class ScheduledMethodRunnable implements SchedulingAwareRunnable {

  private final Object target;

  private final Method method;

  @Nullable
  private final String qualifier;

  /**
   * Create a {@code ScheduledMethodRunnable} for the given target instance,
   * calling the specified method.
   *
   * @param target the target instance to call the method on
   * @param method the target method to call
   * @param qualifier a qualifier associated with this Runnable,
   * e.g. for determining a scheduler to run this scheduled method on
   */
  public ScheduledMethodRunnable(Object target, Method method, @Nullable String qualifier) {
    this.target = target;
    this.method = method;
    this.qualifier = qualifier;
  }

  /**
   * Create a {@code ScheduledMethodRunnable} for the given target instance,
   * calling the specified method.
   *
   * @param target the target instance to call the method on
   * @param method the target method to call
   */
  public ScheduledMethodRunnable(Object target, Method method) {
    this(target, method, null);
  }

  /**
   * Create a {@code ScheduledMethodRunnable} for the given target instance,
   * calling the specified method by name.
   *
   * @param target the target instance to call the method on
   * @param methodName the name of the target method
   * @throws NoSuchMethodException if the specified method does not exist
   */
  public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
    this(target, target.getClass().getMethod(methodName));
  }

  /**
   * Return the target instance to call the method on.
   */
  public Object getTarget() {
    return this.target;
  }

  /**
   * Return the target method to call.
   */
  public Method getMethod() {
    return this.method;
  }

  @Override
  @Nullable
  public String getQualifier() {
    return this.qualifier;
  }

  @Override
  public void run() {
    try {
      ReflectionUtils.makeAccessible(this.method);
      this.method.invoke(this.target);
    }
    catch (InvocationTargetException ex) {
      ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
    }
    catch (IllegalAccessException ex) {
      throw new UndeclaredThrowableException(ex);
    }
  }

  @Override
  public String toString() {
    return this.method.getDeclaringClass().getName() + "." + this.method.getName();
  }

}
