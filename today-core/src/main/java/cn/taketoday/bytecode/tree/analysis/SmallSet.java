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
package cn.taketoday.bytecode.tree.analysis;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An immutable set of at most two elements, optimized for speed compared to a generic set
 * implementation.
 *
 * @author Eric Bruneton
 */
final class SmallSet<T> extends AbstractSet<T> {

  /** The first element of this set, maybe {@literal null}. */
  private final T element1;

  /**
   * The second element of this set, maybe {@literal null}. If {@link #element1} is {@literal null}
   * then this field must be {@literal null}, otherwise it must be different from {@link #element1}.
   */
  private final T element2;

  // -----------------------------------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------------------------------

  /** Constructs an empty set. */
  SmallSet() {
    this.element1 = null;
    this.element2 = null;
  }

  /**
   * Constructs a set with exactly one element.
   *
   * @param element the unique set element.
   */
  SmallSet(final T element) {
    this.element1 = element;
    this.element2 = null;
  }

  /**
   * Constructs a new {@link SmallSet}.
   *
   * @param element1 see {@link #element1}.
   * @param element2 see {@link #element2}.
   */
  private SmallSet(final T element1, final T element2) {
    this.element1 = element1;
    this.element2 = element2;
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the inherited abstract methods
  // -----------------------------------------------------------------------------------------------

  @Override
  public Iterator<T> iterator() {
    return new IteratorImpl<>(element1, element2);
  }

  @Override
  public int size() {
    if (element1 == null) {
      return 0;
    }
    else if (element2 == null) {
      return 1;
    }
    else {
      return 2;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the union of this set and of the given set.
   *
   * @param otherSet another small set.
   * @return the union of this set and of otherSet.
   */
  Set<T> union(final SmallSet<T> otherSet) {
    // If the two sets are equal, return this set.
    if ((otherSet.element1 == element1 && otherSet.element2 == element2)
            || (otherSet.element1 == element2 && otherSet.element2 == element1)) {
      return this;
    }
    // If one set is empty, return the other.
    if (otherSet.element1 == null) {
      return this;
    }
    if (element1 == null) {
      return otherSet;
    }

    // At this point we know that the two sets are non empty and are different.
    // If otherSet contains exactly one element:
    if (otherSet.element2 == null) {
      // If this set also contains exactly one element, we have two distinct elements.
      if (element2 == null) {
        return new SmallSet<>(element1, otherSet.element1);
      }
      // If otherSet is included in this set, return this set.
      if (otherSet.element1 == element1 || otherSet.element1 == element2) {
        return this;
      }
    }
    // If this set contains exactly one element, then otherSet contains two elements (because of the
    // above tests). Thus, if otherSet contains this set, return otherSet:
    if (element2 == null && (element1 == otherSet.element1 || element1 == otherSet.element2)) {
      return otherSet;
    }

    // At this point we know that there are at least 3 distinct elements, so we need a generic set
    // to store the result.
    HashSet<T> result = new HashSet<>(4);
    result.add(element1);
    if (element2 != null) {
      result.add(element2);
    }
    result.add(otherSet.element1);
    if (otherSet.element2 != null) {
      result.add(otherSet.element2);
    }
    return result;
  }

  static class IteratorImpl<T> implements Iterator<T> {

    /** The next element to return in {@link #next}. Maybe {@literal null}. */
    private T firstElement;

    /**
     * The element to return in {@link #next}, after {@link #firstElement} is returned. If {@link
     * #firstElement} is {@literal null} then this field must be {@literal null}, otherwise it must
     * be different from {@link #firstElement}.
     */
    private T secondElement;

    IteratorImpl(final T firstElement, final T secondElement) {
      this.firstElement = firstElement;
      this.secondElement = secondElement;
    }

    @Override
    public boolean hasNext() {
      return firstElement != null;
    }

    @Override
    public T next() {
      if (firstElement == null) {
        throw new NoSuchElementException();
      }
      T element = firstElement;
      firstElement = secondElement;
      secondElement = null;
      return element;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
