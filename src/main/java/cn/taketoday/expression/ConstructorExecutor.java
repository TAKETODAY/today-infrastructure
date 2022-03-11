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

// TODO Is the resolver/executor model too pervasive in this package?

/**
 * Executors are built by resolvers and can be cached by the infrastructure to repeat an
 * operation quickly without going back to the resolvers. For example, the particular
 * constructor to run on a class may be discovered by the reflection constructor resolver
 * - it will then build a ConstructorExecutor that executes that constructor and the
 * ConstructorExecutor can be reused without needing to go back to the resolver to
 * discover the constructor again.
 *
 * <p>They can become stale, and in that case should throw an AccessException - this will
 * cause the infrastructure to go back to the resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @since 4.0
 */
public interface ConstructorExecutor {

  /**
   * Execute a constructor in the specified context using the specified arguments.
   *
   * @param context the evaluation context in which the command is being executed
   * @param arguments the arguments to the constructor call, should match (in terms
   * of number and type) whatever the command will need to run
   * @return the new object
   * @throws AccessException if there is a problem executing the command or the
   * CommandExecutor is no longer valid
   */
  TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
