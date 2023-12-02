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
package cn.taketoday.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.taketoday.lang.Nullable;

/**
 * Iterator over an array.
 *
 * @author TODAY 2021/10/21 14:32
 */
public class ArrayIterator<E> implements Iterator<E>, Enumeration<E>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private int offset;

  private final E[] array;

  private final int endNdx;

  public ArrayIterator(final E[] array) {
    this(array, 0, array.length);
  }

  public ArrayIterator(final E[] array, final int offset, final int len) {
    this.offset = offset;
    this.array = array;
    this.endNdx = offset + len;
  }

  @Override
  public boolean hasNext() {
    return offset < endNdx;
  }

  @Nullable
  @Override
  public E next() throws NoSuchElementException {
    if (offset < endNdx) {
      return array[offset++];
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasMoreElements() {
    return offset < endNdx;
  }

  @Nullable
  @Override
  public E nextElement() {
    return next();
  }

  @Override
  public Iterator<E> asIterator() {
    return this;
  }

}
