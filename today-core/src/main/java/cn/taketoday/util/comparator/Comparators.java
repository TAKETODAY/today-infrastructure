/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.util.comparator;

import java.util.Comparator;

/**
 * Convenient entry point with generically typed factory methods
 * for common {@link Comparator} variants.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class Comparators {

  /**
   * Return a {@link Comparable} adapter.
   *
   * @see Comparator#naturalOrder()
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> comparable() {
    return (Comparator<T>) Comparator.naturalOrder();
  }

  /**
   * Return a {@link Comparable} adapter which accepts
   * null values and sorts them lower than non-null values.
   *
   * @see Comparator#nullsFirst(Comparator)
   */
  public static <T> Comparator<T> nullsLow() {
    return nullsLow(comparable());
  }

  /**
   * Return a decorator for the given comparator which accepts
   * null values and sorts them lower than non-null values.
   *
   * @see Comparator#nullsFirst(Comparator)
   */
  public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
    return Comparator.nullsFirst(comparator);
  }

  /**
   * Return a {@link Comparable} adapter which accepts
   * null values and sorts them higher than non-null values.
   *
   * @see Comparator#nullsLast(Comparator)
   */
  public static <T> Comparator<T> nullsHigh() {
    return nullsHigh(comparable());
  }

  /**
   * Return a decorator for the given comparator which accepts
   * null values and sorts them higher than non-null values.
   *
   * @see Comparator#nullsLast(Comparator)
   */
  public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
    return Comparator.nullsLast(comparator);
  }

}
