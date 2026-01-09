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

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import infra.core.TypeDescriptor;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.MethodExecutor;
import infra.expression.MethodResolver;

/**
 * A {@link MethodResolver} variant for data binding
 * purposes, using reflection to access instance methods on a given target object.
 *
 * <p>This accessor does not resolve static methods and also no technical methods
 * on {@code java.lang.Object} or {@code java.lang.Class}.
 * For unrestricted resolution, choose {@link ReflectiveMethodResolver} instead.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
