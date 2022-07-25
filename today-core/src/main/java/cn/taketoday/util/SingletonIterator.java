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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author TODAY 2021/10/6 17:30
 * @since 4.0
 */
public final class SingletonIterator<E> implements Iterator<E> {
  private boolean hasNext = true;
  final E element;

  public SingletonIterator(E element) {
    this.element = element;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public E next() {
    if (hasNext) {
      hasNext = false;
      return element;
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEachRemaining(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    if (hasNext) {
      action.accept(element);
      hasNext = false;
    }
  }
}

