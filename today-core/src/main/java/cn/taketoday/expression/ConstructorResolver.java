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
 * A constructor resolver attempts to locate a constructor and returns a
 * {@link ConstructorExecutor} that can be used to invoke that constructor.
 *
 * <p>The {@code ConstructorExecutor} will be cached, but if it becomes stale the
 * resolvers will be called again.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConstructorExecutor
 * @see MethodResolver
 * @since 4.0
 */
@FunctionalInterface
public interface ConstructorResolver {

  /**
   * Within the supplied context, resolve a suitable constructor on the
   * supplied type that can handle the specified arguments.
   * <p>Returns a {@link ConstructorExecutor} that can be used to invoke that
   * constructor (or {@code null} if no constructor could be found).
   *
   * @param context the current evaluation context
   * @param typeName the fully-qualified name of the type upon which to look
   * for the constructor
   * @param argumentTypes the types of arguments that the constructor must be
   * able to handle
   * @return a {@code ConstructorExecutor} that can invoke the constructor,
   * or {@code null} if the constructor cannot be found
   */
  @Nullable
  ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
          throws AccessException;

}
