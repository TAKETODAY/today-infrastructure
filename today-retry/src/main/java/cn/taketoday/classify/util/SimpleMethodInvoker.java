/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.classify.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;

/**
 * Simple implementation of the {@link MethodInvoker} interface that invokes a method on
 * an object. If the method has no arguments, but arguments are provided, they are ignored
 * and the method is invoked anyway. If there are more arguments than there are provided,
 * then an exception is thrown.
 *
 * @author Lucas Ward
 * @author Artem Bilan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleMethodInvoker implements MethodInvoker {

  private final Object object;

  private final Method method;

  private final Class<?>[] parameterTypes;

  private volatile Object target;

  public SimpleMethodInvoker(Object object, Method method) {
    Assert.notNull(object, "Object to invoke is required");
    Assert.notNull(method, "Method to invoke is required");
    this.method = method;
    this.object = object;
    this.parameterTypes = method.getParameterTypes();

    ReflectionUtils.makeAccessible(method);
  }

  public SimpleMethodInvoker(Object object, String methodName, Class<?>... paramTypes) {
    Assert.notNull(object, "Object to invoke is required");
    Method method = ReflectionUtils.getMethodIfAvailable(object.getClass(), methodName, paramTypes);
    if (method == null) {
      // try with no params
      method = ReflectionUtils.getMethodIfAvailable(object.getClass(), methodName);
    }

    if (method == null) {
      throw new IllegalArgumentException(
              "No methods found for name: [" + methodName + "] in class: [" + object.getClass()
                      + "] with arguments of type: [" + Arrays.toString(paramTypes) + "]");
    }

    this.object = object;
    this.method = method;
    this.parameterTypes = method.getParameterTypes();
    ReflectionUtils.makeAccessible(method);
  }

  /*
   * (non-Javadoc)
   *
   * @see cn.taketoday.batch.core.configuration.util.MethodInvoker#invokeMethod
   * (java.lang.Object[])
   */
  @Override
  public Object invokeMethod(Object... args) {
    if (this.parameterTypes.length != args.length) {
      throw new IllegalStateException(
              "Wrong number of arguments, expected no more than: [" + this.parameterTypes.length + "]");
    }

    try {
      // Extract the target from an Advised as late as possible
      // in case it contains a lazy initialization
      Object target = extractTarget(this.object, this.method);
      return method.invoke(target, args);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Unable to invoke method: [" + this.method + "] on object: ["
              + this.object + "] with arguments: [" + Arrays.toString(args) + "]", e);
    }
  }

  private Object extractTarget(Object target, Method method) {
    if (this.target == null) {
      if (target instanceof Advised) {
        Object source;
        try {
          source = ((Advised) target).getTargetSource().getTarget();
        }
        catch (Exception e) {
          throw new IllegalStateException("Could not extract target from proxy", e);
        }
        if (source instanceof Advised) {
          source = extractTarget(source, method);
        }
        if (method.getDeclaringClass().isAssignableFrom(source.getClass())) {
          target = source;
        }
      }
      this.target = target;

    }
    return this.target;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return obj instanceof SimpleMethodInvoker rhs
            && rhs.method.equals(this.method)
            && rhs.object.equals(this.object);
  }

  @Override
  public int hashCode() {
    return Objects.hash(object, method);
  }

}
