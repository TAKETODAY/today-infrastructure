/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Contract for {@link RuntimeHints runtime hints} that only apply
 * if the described condition is met.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public interface ConditionalHint {

  /**
   * Return the type that should be reachable for this hint to apply, or
   * {@code null} if this hint should always been applied.
   *
   * @return the reachable type, if any
   */
  @Nullable
  TypeReference getReachableType();

  /**
   * Whether the condition described for this hint is met. If it is not,
   * the hint does not apply.
   * <p>Instead of checking for actual reachability of a type in the
   * application, the classpath is checked for the presence of this
   * type as a simple heuristic.
   *
   * @param classLoader the current classloader
   * @return whether the condition is met and the hint applies
   */
  default boolean conditionMatches(ClassLoader classLoader) {
    TypeReference reachableType = getReachableType();
    if (reachableType != null) {
      return ClassUtils.isPresent(reachableType.getCanonicalName(), classLoader);
    }
    return true;
  }

}
