// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode.tree.analysis;

import java.util.AbstractList;

/**
 * A {@link java.util.List} implementation which checks its allocations with a {@link
 * ComputeLimits}.
 *
 * @param <T> the list element type.
 */
final class CheckedArrayList<T> extends AbstractList<T> {

  /** The list elements. */
  private T[] elements;

  /** The actual number of elements in {@link #elements}. */
  private int size;

  /** The limits for memory allocations. */
  private final ComputeLimits limits;

  @SuppressWarnings("unchecked")
  CheckedArrayList(final ComputeLimits limits) {
    this.elements = (T[]) limits.checkNewObjectArray(4);
    this.size = 0;
    this.limits = limits;
  }

  CheckedArrayList(final CheckedArrayList<T> other) {
    this.elements = other.elements.clone();
    this.size = other.size;
    this.limits = other.limits;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public T get(final int index) {
    return elements[index];
  }

  @Override
  public T set(final int index, final T value) {
    T oldValue = elements[index];
    elements[index] = value;
    return oldValue;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean add(final T value) {
    if (size == elements.length) {
      T[] newElements = (T[]) limits.checkNewObjectArray(2 * size);
      System.arraycopy(elements, 0, newElements, 0, size);
      elements = newElements;
    }
    elements[size++] = value;
    return true;
  }

  @Override
  public T remove(final int index) {
    size--;
    T result = elements[index];
    if (index < size) {
      System.arraycopy(elements, index + 1, elements, index, size - index);
    }
    elements[size] = null;
    return result;
  }
}
