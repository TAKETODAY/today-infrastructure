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
 * Compares objects based on an arbitrary class order. Allows objects to be sorted based
 * on the types of class that they inherit &mdash; for example, this comparator can be used
 * to sort a list of {@code Number}s such that {@code Long}s occur before {@code Integer}s.
 *
 * <p>Only the specified {@code instanceOrder} classes are considered during comparison.
 * If two objects are both instances of the ordered type this comparator will return a
 * value of {@code 0}. Consider combining with {@link Comparator#thenComparing(Comparator)}
 * if additional sorting is required.
 *
 * @param <T> the type of objects that may be compared by this comparator
 * @author Phillip Webb
 * @see Comparator#thenComparing(Comparator)
 * @since 4.0
 */
public class InstanceComparator<T> implements Comparator<T> {

  private final Class<?>[] instanceOrder;

  /**
   * Create a new {@link InstanceComparator} instance.
   *
   * @param instanceOrder the ordered list of classes that should be used when comparing
   * objects. Classes earlier in the list will be given a higher priority.
   */
  public InstanceComparator(Class<?>... instanceOrder) {
    Assert.notNull(instanceOrder, "'instanceOrder' array must not be null");
    this.instanceOrder = instanceOrder;
  }

  @Override
  public int compare(T o1, T o2) {
    int i1 = getOrder(o1);
    int i2 = getOrder(o2);
    return (Integer.compare(i1, i2));
  }

  private int getOrder(@Nullable T object) {
    if (object != null) {
      for (int i = 0; i < this.instanceOrder.length; i++) {
        if (this.instanceOrder[i].isInstance(object)) {
          return i;
        }
      }
    }
    return this.instanceOrder.length;
  }

}
