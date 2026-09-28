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

package infra.persistence;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import infra.util.Assert;

/**
 * Iterates over the elements of successive {@link Scroll windows}, fetching the
 * next window on demand and applying the {@link ScrollPosition} automatically.
 *
 * <p>Usage:
 * <pre>{@code
 * ScrollIterator<User> users = ScrollIterator
 *     .of(position -> entityManager.scroll(User.class, ScrollPageable.of(10).withPosition(position)));
 *
 * while (users.hasNext()) {
 *   User user = users.next();
 *   // consume
 * }
 * }</pre>
 *
 * @param <T> the element type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Scroll
 * @since 5.0
 */
public final class ScrollIterator<T> implements Iterator<T> {

  private final Function<ScrollPosition, Scroll<T>> windowSupplier;

  private final ScrollPosition position;

  private @Nullable Scroll<T> scroll;

  private Iterator<T> iterator = Collections.emptyIterator();

  private boolean exhausted;

  private ScrollIterator(Function<ScrollPosition, Scroll<T>> windowSupplier, ScrollPosition position) {
    Assert.notNull(windowSupplier, "Scroll supplier is required");
    Assert.notNull(position, "ScrollPosition is required");
    this.windowSupplier = windowSupplier;
    this.position = position;
  }

  /**
   * Create an iterator backed by the given window supplier.
   *
   * @param windowSupplier a function returning the window at a scroll position
   * @param <T> the element type
   * @return a new window iterator positioned at the start of the scroll operation
   */
  public static <T> ScrollIterator<T> of(Function<ScrollPosition, Scroll<T>> windowSupplier) {
    return of(windowSupplier, ScrollPosition.keyset());
  }

  /**
   * Create an iterator that resumes after the given position.
   *
   * @param windowSupplier a function returning the window at a scroll position
   * @param position the position to resume after
   * @param <T> the element type
   * @return a new window iterator starting after the given position
   */
  public static <T> ScrollIterator<T> of(Function<ScrollPosition, Scroll<T>> windowSupplier, ScrollPosition position) {
    return new ScrollIterator<>(windowSupplier, position);
  }

  @Override
  public boolean hasNext() {
    if (iterator.hasNext()) {
      return true;
    }
    if (exhausted) {
      return false;
    }
    fetchScroll();
    return iterator.hasNext();
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return iterator.next();
  }

  private void fetchScroll() {
    if (scroll == null) {
      scroll = windowSupplier.apply(position);
    }
    else if (scroll.isLast()) {
      exhausted = true;
      return;
    }
    else {
      scroll = windowSupplier.apply(scroll.position());
    }

    iterator = scroll.rows().iterator();
    if (!iterator.hasNext()) {
      exhausted = true;
    }
  }

}
