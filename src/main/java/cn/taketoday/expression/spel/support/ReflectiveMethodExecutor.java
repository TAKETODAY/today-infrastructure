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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.MethodExecutor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link MethodExecutor} that works via reflection.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ReflectiveMethodExecutor implements MethodExecutor {

  private final Method originalMethod;

  private final Method methodToInvoke;

  @Nullable
  private final Integer varargsPosition;

  private boolean computedPublicDeclaringClass = false;

  @Nullable
  private Class<?> publicDeclaringClass;

  private boolean argumentConversionOccurred = false;

  /**
   * Create a new executor for the given method.
   *
   * @param method the method to invoke
   */
  public ReflectiveMethodExecutor(Method method) {
    this(method, null);
  }

  /**
   * Create a new executor for the given method.
   *
   * @param method the method to invoke
   * @param targetClass the target class to invoke the method on
   */
  public ReflectiveMethodExecutor(Method method, @Nullable Class<?> targetClass) {
    this.originalMethod = method;
    this.methodToInvoke = ReflectionUtils.getInterfaceMethodIfPossible(method, targetClass);
    if (method.isVarArgs()) {
      this.varargsPosition = method.getParameterCount() - 1;
    }
    else {
      this.varargsPosition = null;
    }
  }

  /**
   * Return the original method that this executor has been configured for.
   */
  public final Method getMethod() {
    return this.originalMethod;
  }

  /**
   * Find the first public class in the methods declaring class hierarchy that declares this method.
   * Sometimes the reflective method discovery logic finds a suitable method that can easily be
   * called via reflection but cannot be called from generated code when compiling the expression
   * because of visibility restrictions. For example if a non-public class overrides toString(),
   * this helper method will walk up the type hierarchy to find the first public type that declares
   * the method (if there is one!). For toString() it may walk as far as Object.
   */
  @Nullable
  public Class<?> getPublicDeclaringClass() {
    if (!this.computedPublicDeclaringClass) {
      this.publicDeclaringClass =
              discoverPublicDeclaringClass(this.originalMethod, this.originalMethod.getDeclaringClass());
      this.computedPublicDeclaringClass = true;
    }
    return this.publicDeclaringClass;
  }

  @Nullable
  private Class<?> discoverPublicDeclaringClass(Method method, Class<?> clazz) {
    if (Modifier.isPublic(clazz.getModifiers())) {
      try {
        clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        return clazz;
      }
      catch (NoSuchMethodException ex) {
        // Continue below...
      }
    }
    if (clazz.getSuperclass() != null) {
      return discoverPublicDeclaringClass(method, clazz.getSuperclass());
    }
    return null;
  }

  public boolean didArgumentConversionOccur() {
    return this.argumentConversionOccurred;
  }

  @Override
  public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
    try {
      this.argumentConversionOccurred = ReflectionHelper.convertArguments(
              context.getTypeConverter(), arguments, this.originalMethod, this.varargsPosition);
      if (this.originalMethod.isVarArgs()) {
        arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
                this.originalMethod.getParameterTypes(), arguments);
      }
      ReflectionUtils.makeAccessible(this.methodToInvoke);
      Object value = this.methodToInvoke.invoke(target, arguments);
      return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.originalMethod, -1)).narrow(value));
    }
    catch (Exception ex) {
      throw new AccessException("Problem invoking method: " + this.methodToInvoke, ex);
    }
  }

}
