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

package cn.taketoday.expression;

import java.util.List;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * A method resolver attempts to locate a method and returns a
 * {@link MethodExecutor} that can be used to invoke that method.
 *
 * <p>The {@code MethodExecutor} will be cached, but if it becomes stale the
 * resolvers will be called again.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodExecutor
 * @see ConstructorResolver
 * @since 4.0
 */
public interface MethodResolver {

  /**
   * Within the supplied context, resolve a suitable method on the supplied
   * object that can handle the specified arguments.
   * <p>Returns a {@link MethodExecutor} that can be used to invoke that method,
   * or {@code null} if no method could be found.
   *
   * @param context the current evaluation context
   * @param targetObject the object upon which the method is being called
   * @param name the name of the method
   * @param argumentTypes the types of arguments that the method must be able
   * to handle
   * @return a {@code MethodExecutor} that can invoke the method, or {@code null}
   * if the method cannot be found
   */
  @Nullable
  MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
          List<TypeDescriptor> argumentTypes) throws AccessException;

}
