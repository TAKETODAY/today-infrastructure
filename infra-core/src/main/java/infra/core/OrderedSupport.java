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

package infra.core;

import org.jspecify.annotations.Nullable;

/**
 * Convenient base class for {@link Ordered} implementations with a configurable
 * order value.
 *
 * <p>Defaults to {@link Ordered#LOWEST_PRECEDENCE} when no order value has been
 * explicitly configured. Lower values indicate higher priority.
 *
 * @author TODAY 2019-12-27 11:31
 * @see #setOrder(int)
 */
public class OrderedSupport implements Ordered {

  /**
   * The configured order value, or {@code null} if no order has been set.
   *
   * @see #getOrder()
   */
  protected @Nullable Integer order;

  /**
   * Create a new instance with the default order of
   * {@link Ordered#LOWEST_PRECEDENCE}.
   */
  public OrderedSupport() {
  }

  /**
   * Create a new instance with the specified order value.
   *
   * @param order the order value; lower values indicate higher priority
   */
  public OrderedSupport(int order) {
    this.order = order;
  }

  /**
   * Return the configured order value, or {@link Ordered#LOWEST_PRECEDENCE}
   * if no order value has been set.
   *
   * @return the order value
   */
  @Override
  public int getOrder() {
    return order == null ? LOWEST_PRECEDENCE : order;
  }

  /**
   * Set the order value for this instance.
   *
   * @param order the order value; lower values indicate higher priority
   * @see #getOrder()
   */
  public void setOrder(int order) {
    this.order = order;
  }

}
