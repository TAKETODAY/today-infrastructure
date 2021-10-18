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

package cn.taketoday.core;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface to provide an order source for a given object.
 *
 * @author TODAY 2021/9/12 11:34
 * @since 4.0
 */
@FunctionalInterface
public interface OrderSourceProvider {

  /**
   * Return an order source for the specified object, i.e. an object that
   * should be checked for an order value as a replacement to the given object.
   * <p>Can also be an array of order source objects.
   * <p>If the returned object does not indicate any order, the comparator
   * will fall back to checking the original object.
   *
   * @param obj the object to find an order source for
   * @return the order source for that object, or {@code null} if none found
   */
  @Nullable
  Object getOrderSource(Object obj);
}
