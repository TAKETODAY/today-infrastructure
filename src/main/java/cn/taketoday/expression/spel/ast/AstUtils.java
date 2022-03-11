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

package cn.taketoday.expression.spel.ast;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.lang.Nullable;

/**
 * Utilities methods for use in the Ast classes.
 *
 * @author Andy Clement
 * @since 4.0
 */
public abstract class AstUtils {

  /**
   * Determines the set of property resolvers that should be used to try and access a
   * property on the specified target type. The resolvers are considered to be in an
   * ordered list, however in the returned list any that are exact matches for the input
   * target type (as opposed to 'general' resolvers that could work for any type) are
   * placed at the start of the list. In addition, there are specific resolvers that
   * exactly name the class in question and resolvers that name a specific class but it
   * is a supertype of the class we have. These are put at the end of the specific resolvers
   * set and will be tried after exactly matching accessors but before generic accessors.
   *
   * @param targetType the type upon which property access is being attempted
   * @return a list of resolvers that should be tried in order to access the property
   */
  public static List<PropertyAccessor> getPropertyAccessorsToTry(
          @Nullable Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

    ArrayList<PropertyAccessor> specificAccessors = new ArrayList<>();
    ArrayList<PropertyAccessor> generalAccessors = new ArrayList<>();
    for (PropertyAccessor resolver : propertyAccessors) {
      Class<?>[] targets = resolver.getSpecificTargetClasses();
      if (targets == null) {  // generic resolver that says it can be used for any type
        generalAccessors.add(resolver);
      }
      else {
        if (targetType != null) {
          for (Class<?> clazz : targets) {
            if (clazz == targetType) {  // put exact matches on the front to be tried first?
              specificAccessors.add(resolver);
            }
            else if (clazz.isAssignableFrom(targetType)) {  // put supertype matches at the end of the
              // specificAccessor list
              generalAccessors.add(resolver);
            }
          }
        }
      }
    }
    ArrayList<PropertyAccessor> resolvers = new ArrayList<>(specificAccessors.size() + generalAccessors.size());
    resolvers.addAll(specificAccessors);
    resolvers.addAll(generalAccessors);
    return resolvers;
  }

}
