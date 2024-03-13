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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Method;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReflectiveMethodExecutor implements MethodExecutor {

  private final Method originalMethod;

  /**
   * The method to invoke via reflection, which is not necessarily the method
   * to invoke in a compiled expression.
   */
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
   * Find a public class or interface in the method's class hierarchy that
   * declares the {@linkplain #getMethod() original method}.
   * <p>See {@link ReflectionHelper#findPublicDeclaringClass(Method)} for
   * details.
   *
   * @return the public class or interface that declares the method, or
   * {@code null} if no such public type could be found
   */
  @Nullable
  public Class<?> getPublicDeclaringClass() {
    if (!this.computedPublicDeclaringClass) {
      this.publicDeclaringClass = ReflectionHelper.findPublicDeclaringClass(this.originalMethod);
      this.computedPublicDeclaringClass = true;
    }
    return this.publicDeclaringClass;
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
