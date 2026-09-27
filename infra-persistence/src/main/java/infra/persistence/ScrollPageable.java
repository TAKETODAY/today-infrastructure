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

import infra.persistence.sql.OrderSpec;
import infra.util.Assert;

/**
 * Request parameters for a {@linkplain EntityManager#scroll scroll} operation:
 * the window size and an optional explicit keyset order. The scroll position is
 * supplied separately, since it is a coordinate rather than a request setting.
 *
 * <p>When no {@link #order() order} is given, the keyset order is resolved from
 * the entity's {@link infra.persistence.annotation.Keyset @Keyset} declarations
 * and then the query condition.
 *
 * @param pageSize the maximum number of rows per window, between 1 and
 * {@code Integer.MAX_VALUE - 1}
 * @param order explicit keyset order, or {@code null} to use the entity or condition ordering
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ScrollPosition
 * @since 5.0
 */
public record ScrollPageable(int pageSize, @Nullable OrderSpec order) {

  public ScrollPageable {
    Assert.isTrue(pageSize > 0 && pageSize < Integer.MAX_VALUE,
            "Page size must be between 1 and Integer.MAX_VALUE - 1");
  }

  /**
   * Create a request with the given window size and no explicit order.
   *
   * @param pageSize the maximum number of rows per window
   * @return a scroll request
   */
  public static ScrollPageable of(int pageSize) {
    return new ScrollPageable(pageSize, null);
  }

  /**
   * Return a request with the given explicit keyset order.
   *
   * @param order the keyset ordering to apply
   * @return a request with the given ordering
   */
  public ScrollPageable withOrder(OrderSpec order) {
    Assert.notNull(order, "OrderSpec is required");
    return new ScrollPageable(pageSize, order);
  }

}
