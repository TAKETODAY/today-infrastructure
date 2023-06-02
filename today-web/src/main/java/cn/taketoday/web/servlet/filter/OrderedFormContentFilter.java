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

package cn.taketoday.web.servlet.filter;

import cn.taketoday.core.Ordered;

/**
 * {@link FormContentFilter} that also implements {@link Ordered}.
 *
 * @author Joao Pedro Evangelista
 * @author Brian Clozel
 * @since 4.0
 */
public class OrderedFormContentFilter extends FormContentFilter implements OrderedFilter {

  /**
   * Higher order to ensure the filter is applied before Framework Security.
   */
  public static final int DEFAULT_ORDER = REQUEST_WRAPPER_FILTER_MAX_ORDER - 9900;

  private int order = DEFAULT_ORDER;

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Set the order for this filter.
   *
   * @param order the order to set
   */
  public void setOrder(int order) {
    this.order = order;
  }

}
