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
 * A {@code ConstructorExecutor} is built by a {@link ConstructorResolver} and
 * can be cached by the infrastructure to repeat an operation quickly without
 * going back to the resolvers.
 *
 * <p>For example, the particular constructor to execute on a class may be discovered
 * by a {@code ConstructorResolver} which then builds a {@code ConstructorExecutor}
 * that executes that constructor, and the resolved {@code ConstructorExecutor}
 * can be reused without needing to go back to the resolvers to discover the
 * constructor again.
 *
 * <p>If a {@code ConstructorExecutor} becomes stale, it should throw an
 * {@link AccessException} which signals to the infrastructure to go back to the
 * resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConstructorExecutor {

  /**
   * Execute a constructor in the specified context using the specified arguments.
   *
   * @param context the evaluation context in which the constructor is being executed
   * @param arguments the arguments to the constructor; should match (in terms
   * of number and type) whatever the constructor will need to run
   * @return the new object
   * @throws AccessException if there is a problem executing the constructor or
   * if this {@code ConstructorExecutor} has become stale
   */
  TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
