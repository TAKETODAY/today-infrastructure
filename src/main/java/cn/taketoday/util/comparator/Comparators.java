/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util.comparator;

import java.util.Comparator;

/**
 * Convenient entry point with generically typed factory methods
 * for common {@link Comparator} variants.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class Comparators {

  /**
   * Return a {@link Comparable} adapter.
   *
   * @see ComparableComparator#INSTANCE
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> comparable() {
    return ComparableComparator.INSTANCE;
  }

  /**
   * Return a {@link Comparable} adapter which accepts
   * null values and sorts them lower than non-null values.
   *
   * @see NullSafeComparator#NULLS_LOW
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> nullsLow() {
    return NullSafeComparator.NULLS_LOW;
  }

  /**
   * Return a decorator for the given comparator which accepts
   * null values and sorts them lower than non-null values.
   *
   * @see NullSafeComparator#NullSafeComparator(boolean)
   */
  public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
    return new NullSafeComparator<>(comparator, true);
  }

  /**
   * Return a {@link Comparable} adapter which accepts
   * null values and sorts them higher than non-null values.
   *
   * @see NullSafeComparator#NULLS_HIGH
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> nullsHigh() {
    return NullSafeComparator.NULLS_HIGH;
  }

  /**
   * Return a decorator for the given comparator which accepts
   * null values and sorts them higher than non-null values.
   *
   * @see NullSafeComparator#NullSafeComparator(boolean)
   */
  public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
    return new NullSafeComparator<>(comparator, false);
  }

}
