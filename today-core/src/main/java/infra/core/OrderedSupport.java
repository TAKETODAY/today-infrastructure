/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core;

import org.jspecify.annotations.Nullable;

/**
 * @author TODAY 2019-12-27 11:31
 */
public class OrderedSupport implements Ordered {

  @Nullable
  protected Integer order;  // default: same as non-Ordered

  public OrderedSupport() { }

  public OrderedSupport(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return order == null ? LOWEST_PRECEDENCE : order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

}
