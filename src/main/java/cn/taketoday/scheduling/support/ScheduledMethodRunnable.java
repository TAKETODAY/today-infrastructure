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

package cn.taketoday.scheduling.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import cn.taketoday.util.ReflectionUtils;

/**
 * meant to be used for processing
 * of no-arg scheduled methods. Propagates user exceptions to the caller,
 * assuming that an error strategy for Runnables is in place.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public class ScheduledMethodRunnable implements Runnable {

  private final Object target;
  private final Method method;

  /**
   * Create a {@code ScheduledMethodRunnable} for the given target instance,
   * calling the specified method.
   *
   * @param target the target instance to call the method on
   * @param method the target method to call
   */
  public ScheduledMethodRunnable(Object target, Method method) {
    this.target = target;
    this.method = method;
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
    this.target = target;
    this.method = target.getClass().getMethod(methodName);
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
