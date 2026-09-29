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

import org.jspecify.annotations.Nullable;

/**
 * A source of the {@link ScrollPosition} used to resume a keyset scroll.
 *
 * <p>Example objects can implement this interface to carry their position across
 * an HTTP boundary together with their filtering and ordering state. A
 * {@code null} position uses the position supplied by {@link ScrollPageable}.
 * When both the example and {@code ScrollPageable} supply a position,
 * scrolling fails rather than silently choosing one.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScrollPosition
 * @since 5.0
 */
public interface ScrollPositionSource {

  /**
   * Return the position after which scrolling should resume.
   *
   * @return the scroll position, or {@code null} to use the pageable position
   */
  @Nullable
  ScrollPosition scrollPosition();

}
