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

import java.util.Iterator;
import java.util.List;

/**
 * A chunk of the result of a scroll query, carrying the {@link ScrollPosition}
 * needed to fetch the next chunk.
 *
 * <p>Unlike {@link Slice} or {@link Page}, a window never applies an offset; it
 * resumes from a position captured on one of its own elements. Use
 * {@link #position()} to obtain the position of the last element, or
 * {@link #positionAt(int)} for a specific element, and pass it to the next scroll
 * request. When {@link #isLast()} is {@code true} the window is the final one.
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ScrollPosition
 * @see ScrollIterator
 * @since 5.0
 */
public interface Scroll<T> extends Iterable<T> {

  /**
   * Return the elements contained in this window.
   *
   * @return the rows in this window, never {@code null}
   */
  List<T> rows();

  /**
   * Return the scroll position of the element at the given index.
   *
   * @param index the zero-based index within {@link #rows()}
   * @return the position of that element
   */
  ScrollPosition positionAt(int index);

  /**
   * Return whether this is the final window of the scroll result.
   *
   * @return {@code true} when no further elements follow this window
   */
  boolean isLast();

  /**
   * Return whether this window contains no elements.
   *
   * @return {@code true} when {@link #rows()} is empty
   */
  default boolean isEmpty() {
    return rows().isEmpty();
  }

  /**
   * Return the position of the last element in this window.
   *
   * @return the position of the last element
   * @throws IllegalStateException if this window is empty
   */
  default ScrollPosition position() {
    List<T> rows = rows();
    if (rows.isEmpty()) {
      throw new IllegalStateException("Cannot determine the position of an empty window");
    }
    return positionAt(rows.size() - 1);
  }

  @Override
  default Iterator<T> iterator() {
    return rows().iterator();
  }

}
