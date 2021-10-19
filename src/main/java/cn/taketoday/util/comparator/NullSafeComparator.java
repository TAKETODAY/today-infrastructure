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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A Comparator that will safely compare nulls to be lower or higher than
 * other objects. Can decorate a given Comparator or work on Comparables.
 *
 * @param <T> the type of objects that may be compared by this comparator
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see Comparable
 * @since 4.0
 */
public class NullSafeComparator<T> implements Comparator<T> {

  /**
   * A shared default instance of this comparator, treating nulls lower
   * than non-null objects.
   *
   * @see Comparators#nullsLow()
   */
  @SuppressWarnings("rawtypes")
  public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

  /**
   * A shared default instance of this comparator, treating nulls higher
   * than non-null objects.
   *
   * @see Comparators#nullsHigh()
   */
  @SuppressWarnings("rawtypes")
  public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);

  private final Comparator<T> nonNullComparator;

  private final boolean nullsLow;

  /**
   * Create a NullSafeComparator that sorts {@code null} based on
   * the provided flag, working on Comparables.
   * <p>When comparing two non-null objects, their Comparable implementation
   * will be used: this means that non-null elements (that this Comparator
   * will be applied to) need to implement Comparable.
   * <p>As a convenience, you can use the default shared instances:
   * {@code NullSafeComparator.NULLS_LOW} and
   * {@code NullSafeComparator.NULLS_HIGH}.
   *
   * @param nullsLow whether to treat nulls lower or higher than non-null objects
   * @see Comparable
   * @see #NULLS_LOW
   * @see #NULLS_HIGH
   */
  @SuppressWarnings("unchecked")
  private NullSafeComparator(boolean nullsLow) {
    this.nonNullComparator = ComparableComparator.INSTANCE;
    this.nullsLow = nullsLow;
  }

  /**
   * Create a NullSafeComparator that sorts {@code null} based on the
   * provided flag, decorating the given Comparator.
   * <p>When comparing two non-null objects, the specified Comparator will be used.
   * The given underlying Comparator must be able to handle the elements that this
   * Comparator will be applied to.
   *
   * @param comparator the comparator to use when comparing two non-null objects
   * @param nullsLow whether to treat nulls lower or higher than non-null objects
   */
  public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
    Assert.notNull(comparator, "Non-null Comparator is required");
    this.nonNullComparator = comparator;
    this.nullsLow = nullsLow;
  }

  @Override
  public int compare(@Nullable T o1, @Nullable T o2) {
    if (o1 == o2) {
      return 0;
    }
    if (o1 == null) {
      return (this.nullsLow ? -1 : 1);
    }
    if (o2 == null) {
      return (this.nullsLow ? 1 : -1);
    }
    return this.nonNullComparator.compare(o1, o2);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NullSafeComparator)) {
      return false;
    }
    NullSafeComparator<T> otherComp = (NullSafeComparator<T>) other;
    return (this.nonNullComparator.equals(otherComp.nonNullComparator) && this.nullsLow == otherComp.nullsLow);
  }

  @Override
  public int hashCode() {
    return this.nonNullComparator.hashCode() * (this.nullsLow ? -1 : 1);
  }

  @Override
  public String toString() {
    return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
            (this.nullsLow ? "nulls low" : "nulls high");
  }

}
