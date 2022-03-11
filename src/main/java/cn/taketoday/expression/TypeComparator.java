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

import cn.taketoday.lang.Nullable;

/**
 * Instances of a type comparator should be able to compare pairs of objects for equality.
 * The specification of the return value is the same as for {@link Comparable}.
 *
 * @author Andy Clement
 * @see Comparable
 * @since 4.0
 */
public interface TypeComparator {

  /**
   * Return {@code true} if the comparator can compare these two objects.
   *
   * @param firstObject the first object
   * @param secondObject the second object
   * @return {@code true} if the comparator can compare these objects
   */
  boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject);

  /**
   * Compare two given objects.
   *
   * @param firstObject the first object
   * @param secondObject the second object
   * @return 0 if they are equal, a negative integer if the first is smaller than
   * the second, or a positive integer if the first is larger than the second
   * @throws EvaluationException if a problem occurs during comparison
   * (or if they are not comparable in the first place)
   * @see Comparable#compareTo
   */
  int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException;

}
