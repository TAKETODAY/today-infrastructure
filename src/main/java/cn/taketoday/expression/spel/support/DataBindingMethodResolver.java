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
import java.util.List;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.MethodExecutor;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.lang.Nullable;

/**
 * A {@link MethodResolver} variant for data binding
 * purposes, using reflection to access instance methods on a given target object.
 *
 * <p>This accessor does not resolve static methods and also no technical methods
 * on {@code java.lang.Object} or {@code java.lang.Class}.
 * For unrestricted resolution, choose {@link ReflectiveMethodResolver} instead.
 *
 * @author Juergen Hoeller
 * @see #forInstanceMethodInvocation()
 * @see DataBindingPropertyAccessor
 * @since 4.0
 */
public final class DataBindingMethodResolver extends ReflectiveMethodResolver {

  private DataBindingMethodResolver() {
    super();
  }

  @Override
  @Nullable
  public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
          List<TypeDescriptor> argumentTypes) throws AccessException {

    if (targetObject instanceof Class) {
      throw new IllegalArgumentException("DataBindingMethodResolver does not support Class targets");
    }
    return super.resolve(context, targetObject, name, argumentTypes);
  }

  @Override
  protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
    if (Modifier.isStatic(method.getModifiers())) {
      return false;
    }
    Class<?> clazz = method.getDeclaringClass();
    return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
  }

  /**
   * Create a new data-binding method resolver for instance method resolution.
   */
  public static DataBindingMethodResolver forInstanceMethodInvocation() {
    return new DataBindingMethodResolver();
  }

}
