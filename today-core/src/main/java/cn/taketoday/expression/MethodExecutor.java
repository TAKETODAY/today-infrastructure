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

/**
 * A {@code MethodExecutor} is built by a {@link MethodResolver} and can be cached
 * by the infrastructure to repeat an operation quickly without going back to the
 * resolvers.
 *
 * <p>For example, the particular method to execute on an object may be discovered
 * by a {@code MethodResolver} which then builds a {@code MethodExecutor} that
 * executes that method, and the resolved {@code MethodExecutor} can be reused
 * without needing to go back to the resolvers to discover the method again.
 *
 * <p>If a {@code MethodExecutor} becomes stale, it should throw an
 * {@link AccessException} which signals to the infrastructure to go back to the
 * resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodResolver
 * @see ConstructorExecutor
 * @since 4.0
 */
public interface MethodExecutor {

  /**
   * Execute a method in the specified context using the specified arguments.
   *
   * @param context the evaluation context in which the method is being executed
   * @param target the target of the method invocation; may be {@code null} for
   * {@code static} methods
   * @param arguments the arguments to the method; should match (in terms of
   * number and type) whatever the method will need to run
   * @return the value returned from the method
   * @throws AccessException if there is a problem executing the method or
   * if this {@code MethodExecutor} has become stale
   */
  TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
