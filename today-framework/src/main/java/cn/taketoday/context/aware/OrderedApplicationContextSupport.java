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
package cn.taketoday.context.aware;

import cn.taketoday.core.Ordered;

/**
 * @author TODAY <br>
 * 2020-09-19 21:43
 */
public class OrderedApplicationContextSupport
        extends ApplicationContextSupport implements Ordered {

  private Integer order;

  public OrderedApplicationContextSupport() {
    this(LOWEST_PRECEDENCE);
  }

  public OrderedApplicationContextSupport(int order) {
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
