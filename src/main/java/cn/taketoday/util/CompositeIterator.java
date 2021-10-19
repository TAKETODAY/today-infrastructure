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

package cn.taketoday.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;

import cn.taketoday.lang.Assert;

/**
 * Composite iterator that combines multiple other iterators,
 * as registered via {@link #add(Iterator)}.
 *
 * <p>This implementation maintains a linked set of iterators
 * which are invoked in sequence until all iterators are exhausted.
 *
 * @param <E> the element type
 * @author Erwin Vervaet
 * @author Juergen Hoeller
 * @author TODAY 2021/10/1 14:48
 * @since 4.0
 */
public class CompositeIterator<E> implements Iterator<E> {

  private final LinkedHashSet<Iterator<E>> iterators = new LinkedHashSet<>();

  private boolean inUse = false;

  /**
   * Add given iterator to this composite.
   */
  public void add(Iterator<E> iterator) {
    Assert.state(!this.inUse, "You can no longer add iterators to a composite iterator that's already in use");
    if (this.iterators.contains(iterator)) {
      throw new IllegalArgumentException("You cannot add the same iterator twice");
    }
    this.iterators.add(iterator);
  }

  @Override
  public boolean hasNext() {
    this.inUse = true;
    for (Iterator<E> iterator : this.iterators) {
      if (iterator.hasNext()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public E next() {
    this.inUse = true;
    for (Iterator<E> iterator : this.iterators) {
      if (iterator.hasNext()) {
        return iterator.next();
      }
    }
    throw new NoSuchElementException("All iterators exhausted");
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("CompositeIterator does not support remove()");
  }

}
