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

package cn.taketoday.expression;

/**
 * MethodExecutors are built by the resolvers and can be cached by the infrastructure to
 * repeat an operation quickly without going back to the resolvers. For example, the
 * particular method to run on an object may be discovered by the reflection method
 * resolver - it will then build a MethodExecutor that executes that method and the
 * MethodExecutor can be reused without needing to go back to the resolver to discover
 * the method again.
 *
 * <p>They can become stale, and in that case should throw an AccessException:
 * This will cause the infrastructure to go back to the resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @since 4.0
 */
public interface MethodExecutor {

  /**
   * Execute a command using the specified arguments, and using the specified expression state.
   *
   * @param context the evaluation context in which the command is being executed
   * @param target the target object of the call - null for static methods
   * @param arguments the arguments to the executor, should match (in terms of number
   * and type) whatever the command will need to run
   * @return the value returned from execution
   * @throws AccessException if there is a problem executing the command or the
   * MethodExecutor is no longer valid
   */
  TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
