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

package cn.taketoday.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Nullable;

/**
 * A List which is optimised for the sizes of 0 and 1,
 * in which cases it would not allocate array at all.
 *
 * The tradeoff is the following: this list is slower than {@link ArrayList} but occupies less memory in case of 0 or 1 elements.
 * Please use it only if your code contains many near-empty lists outside the very hot loops.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/4 19:53
 */
public class SmartList<E> extends AbstractList<E> implements RandomAccess, ArraySizeTrimmer {
  private int size;
  private Object element; // null if size==0, (E)element if size==1, Object[] if size>=2

  public SmartList() { }

  public SmartList(E element) {
    this.element = element;
    size = 1;
  }

  public SmartList(Collection<? extends E> elements) {
    int size = elements.size();
    if (size == 1) {
      //noinspection unchecked
      E element = elements instanceof RandomAccess ? ((List<? extends E>) elements).get(0) : elements.iterator().next();
      add(element);
    }
    else if (size > 0) {
      this.size = size;
      element = elements.toArray(new Object[size]);
    }
  }

  @SafeVarargs
  public SmartList(E... elements) {
    int length = elements.length;
    switch (length) {
      case 0:
        break;
      case 1:
        element = elements[0];
        size = 1;
        break;
      default:
        element = Arrays.copyOf(elements, length);
        size = length;
        break;
    }
  }

  @Override
  public E get(int index) {
    int size = this.size;
    checkOutOfBounds(index, size, false);
    if (size == 1) {
      return asElement();
    }
    return asArray()[index];
  }

  private static void checkOutOfBounds(int index, int size, boolean inclusive) {
    if (index < 0 || (inclusive ? index > size : index >= size)) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  @Override
  public boolean add(E e) {
    int size = this.size;
    switch (size) {
      case 0 -> element = e;
      case 1 -> element = new Object[] { element, e };
      default -> {
        E[] array = resizeIfNecessary(size);
        array[size] = e;
      }
    }

    this.size++;
    modCount++;
    return true;
  }

  private E[] resizeIfNecessary(int size) {
    E[] array = asArray();
    int oldCapacity = array.length;
    if (size >= oldCapacity) {
      // have to resize
      int newCapacity = Math.max(oldCapacity * 3 / 2 + 1, size + 1);
      //noinspection unchecked
      element = array = (E[]) realloc(array, newCapacity, Object[]::new);
    }
    return array;
  }

  public static <T> T[] realloc(T[] array, int newSize, IntFunction<T[]> factory) {
    int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    T[] result = factory.apply(newSize);
    if (newSize == 0) {
      return result;
    }

    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Override
  public void add(int index, E e) {
    int size = this.size;
    checkOutOfBounds(index, size, true);
    switch (size) {
      case 0 -> element = e;
      case 1 -> element = index == 0 ? new Object[] { e, element } : new Object[] { element, e };
      default -> {
        E[] array = resizeIfNecessary(size);
        System.arraycopy(array, index, array, index + 1, size - index);
        array[index] = e;
      }
    }

    this.size++;
    modCount++;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    element = null;
    size = 0;
    modCount++;
  }

  @Override
  public E set(final int index, final E element) {
    int size = this.size;
    checkOutOfBounds(index, size, false);
    final E oldValue;
    if (size == 1) {
      oldValue = asElement();
      this.element = element;
    }
    else {
      E[] array = asArray();
      oldValue = array[index];
      array[index] = element;
    }
    return oldValue;
  }

  private E asElement() {
    //noinspection unchecked
    return (E) element;
  }

  private E[] asArray() {
    //noinspection unchecked
    return (E[]) element;
  }

  @Override
  public E remove(final int index) {
    int size = this.size;
    checkOutOfBounds(index, size, false);

    final E oldValue;
    switch (size) {
      case 0: // impossible
      case 1:
        oldValue = asElement();
        element = null;
        break;
      case 2:
        E[] array = asArray();
        oldValue = array[index];
        element = array[1 - index];
        break;
      default:
        array = asArray();
        oldValue = array[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
          System.arraycopy(array, index + 1, array, index, numMoved);
        }
        array[size - 1] = null;
        break;
    }
    this.size--;
    modCount++;
    return oldValue;
  }

  @Override
  public Iterator<E> iterator() {
    return size == 0 ? Collections.emptyIterator() : super.iterator();
  }

  @Override
  public void sort(Comparator<? super E> comparator) {
    if (size >= 2) {
      Arrays.sort(asArray(), 0, size, comparator);
    }
  }

  public int getModificationCount() {
    return modCount;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    int aLength = a.length;
    int size = this.size;
    switch (size) {
      case 0:
        // nothing to copy
        break;
      case 1:
        //noinspection unchecked
        T t = (T) asElement();
        if (aLength == 0) {
          T[] r = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
          r[0] = t;
          return r;
        }
        a[0] = t;
        break;
      default:
        if (aLength < size) {
          //noinspection unchecked
          return (T[]) Arrays.copyOf(asArray(), size, a.getClass());
        }
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(asArray(), 0, a, 0, size);
        break;
    }

    if (aLength > size) {
      a[size] = null;
    }
    return a;
  }

  /**
   * Trims the capacity of this list to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of a list instance.
   */
  @Override
  public void trimToSize() {
    int size = this.size;
    if (size < 2)
      return;
    E[] array = asArray();
    if (size < array.length) {
      modCount++;
      element = Arrays.copyOf(array, size);
    }
  }

  @Override
  public int indexOf(Object o) {
    return switch (size) {
      case 0 -> -1;
      case 1 -> Objects.equals(o, element) ? 0 : -1;
      default -> indexOf(asArray(), o, 0, size);
    };
  }

  public static <T> int indexOf(T[] src, @Nullable T obj, int start, int end) {
    if (obj == null) {
      for (int i = start; i < end; i++) {
        if (src[i] == null)
          return i;
      }
    }
    else {
      for (int i = start; i < end; i++) {
        if (obj.equals(src[i]))
          return i;
      }
    }
    return -1;
  }

  @Override
  public boolean contains(Object o) {
    return indexOf(o) >= 0;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof SmartList) {
      return equalsWithSmartList((SmartList<?>) o);
    }
    if (o instanceof ArrayList) {
      return equalsWithArrayList((ArrayList<?>) o);
    }
    return super.equals(o);
  }

  private boolean equalsWithSmartList(SmartList<?> that) {
    int size = this.size;
    if (size != that.size) {
      return false;
    }
    return switch (size) {
      case 0 -> true;
      case 1 -> Objects.equals(element, that.element);
      default -> compareOneByOne(size, that);
    };
  }

  private boolean equalsWithArrayList(ArrayList<?> that) {
    int size = this.size;
    if (size != that.size()) {
      return false;
    }
    return switch (size) {
      case 0 -> true;
      case 1 -> Objects.equals(element, that.get(0));
      default -> compareOneByOne(size, that);
    };
  }

  private boolean compareOneByOne(int size, List<?> that) {
    E[] array = asArray();
    for (int i = 0; i < size; i++) {
      E o1 = array[i];
      Object o2 = that.get(i);
      if (!Objects.equals(o1, o2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    int size = this.size;
    switch (size) {
      case 0:
        break;
      case 1:
        action.accept(asElement());
        break;
      default:
        E[] array = asArray();
        for (int i = 0; i < size; i++) {
          action.accept(array[i]);
        }
        break;
    }
  }

}
