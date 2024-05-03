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

package cn.taketoday.expression.spel.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.expression.TargetedAccessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Utilities methods for use in the Ast classes.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AstUtils {

  /**
   * Determine the set of accessors that should be used to try to access an
   * element on the specified target object.
   * <p>Delegates to {@link #getAccessorsToTry(Class, List)} with the type of
   * the supplied target object.
   *
   * @param targetObject the object upon which element access is being attempted
   * @param accessors the list of element accessors to process
   * @return a list of accessors that should be tried in order to access the
   * element on the specified target type, or an empty list if no suitable
   * accessor could be found
   */
  static <T extends TargetedAccessor> List<T> getAccessorsToTry(
          @Nullable Object targetObject, List<T> accessors) {

    Class<?> targetType = (targetObject != null ? targetObject.getClass() : null);
    return getAccessorsToTry(targetType, accessors);
  }

  /**
   * Determine the set of accessors that should be used to try to access an
   * element on the specified target type.
   * <p>The accessors are considered to be in an ordered list; however, in the
   * returned list any accessors that are exact matches for the input target
   * type (as opposed to 'generic' accessors that could work for any type) are
   * placed at the start of the list. In addition, if there are specific
   * accessors that exactly name the class in question and accessors that name
   * a specific class which is a supertype of the class in question, the latter
   * are put at the end of the specific accessors set and will be tried after
   * exactly matching accessors but before generic accessors.
   *
   * @param targetType the type upon which element access is being attempted
   * @param accessors the list of element accessors to process
   * @return a list of accessors that should be tried in order to access the
   * element on the specified target type, or an empty list if no suitable
   * accessor could be found
   */
  static <T extends TargetedAccessor> List<T> getAccessorsToTry(@Nullable Class<?> targetType, List<T> accessors) {
    if (accessors.isEmpty()) {
      return Collections.emptyList();
    }

    List<T> exactMatches = new ArrayList<>();
    List<T> inexactMatches = new ArrayList<>();
    List<T> genericMatches = new ArrayList<>();
    for (T accessor : accessors) {
      Class<?>[] targets = accessor.getSpecificTargetClasses();
      if (ObjectUtils.isEmpty(targets)) {
        // generic accessor that says it can be used for any type
        genericMatches.add(accessor);
      }
      else if (targetType != null) {
        for (Class<?> clazz : targets) {
          if (clazz == targetType) {
            exactMatches.add(accessor);
          }
          else if (clazz.isAssignableFrom(targetType)) {
            inexactMatches.add(accessor);
          }
        }
      }
    }

    int size = exactMatches.size() + inexactMatches.size() + genericMatches.size();
    if (size == 0) {
      return Collections.emptyList();
    }
    else {
      ArrayList<T> result = new ArrayList<>(size);
      result.addAll(exactMatches);
      result.addAll(inexactMatches);
      result.addAll(genericMatches);
      return result;
    }
  }

}
