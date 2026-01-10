/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator over an array.
 *
 * @param <E> the type of elements
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
