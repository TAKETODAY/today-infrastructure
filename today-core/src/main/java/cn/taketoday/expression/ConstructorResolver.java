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

import java.util.List;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * A constructor resolver attempts locate a constructor and returns a ConstructorExecutor
 * that can be used to invoke that constructor. The ConstructorExecutor will be cached but
 * if it 'goes stale' the resolvers will be called again.
 *
 * @author Andy Clement
 * @since 4.0
 */
@FunctionalInterface
public interface ConstructorResolver {

  /**
   * Within the supplied context determine a suitable constructor on the supplied type
   * that can handle the specified arguments. Return a ConstructorExecutor that can be
   * used to invoke that constructor (or {@code null} if no constructor could be found).
   *
   * @param context the current evaluation context
   * @param typeName the type upon which to look for the constructor
   * @param argumentTypes the arguments that the constructor must be able to handle
   * @return a ConstructorExecutor that can invoke the constructor, or null if non found
   */
  @Nullable
  ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
          throws AccessException;

}
