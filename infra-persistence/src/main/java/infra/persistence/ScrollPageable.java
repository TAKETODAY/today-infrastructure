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

import infra.util.Assert;

/**
 * Request parameters for a {@linkplain EntityManager#scroll scroll} operation:
 * the window size and the position at which to resume. A {@code null} position
 * starts at the beginning.
 *
 * <p>The keyset order is resolved from the query condition or the entity's
 * {@link infra.persistence.annotation.OrderBy @OrderBy} declarations.
 * Raw {@link infra.persistence.annotation.OrderByClause @OrderByClause} SQL
 * fragments cannot be used as keyset sort keys.
 *
 * @param pageSize the maximum number of rows per window, between 1 and
 * {@code Integer.MAX_VALUE - 1}
 * @param position the position at which to resume scrolling, or {@code null} to start
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ScrollPosition
 * @since 5.0
 */
public record ScrollPageable(int pageSize, @Nullable ScrollPosition position) {

  public ScrollPageable {
    Assert.isTrue(pageSize > 0 && pageSize < Integer.MAX_VALUE,
            "Page size must be between 1 and Integer.MAX_VALUE - 1");
  }

  /**
   * Create a request for the first window with the given window size.
   *
   * @param pageSize the maximum number of rows per window
   * @return a scroll request
   */
  public static ScrollPageable of(int pageSize) {
    return new ScrollPageable(pageSize, null);
  }

  /**
   * Return a request resuming after the given position.
   *
   * @param position the position to resume after
   * @return a request with the given position
   */
  public ScrollPageable withPosition(@Nullable ScrollPosition position) {
    return new ScrollPageable(pageSize, position);
  }

}
