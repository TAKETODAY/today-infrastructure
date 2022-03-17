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

package cn.taketoday.aop.target;

/**
 * Interface to be implemented by dynamic target objects,
 * which support reloading and optionally polling for updates.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author TODAY 2021/2/1 21:19
 * @since 3.0
 */
public interface Refreshable {

  /**
   * Refresh the underlying target object.
   */
  void refresh();

  /**
   * Return the number of actual refreshes since startup.
   */
  long getRefreshCount();

  /**
   * Return the last time an actual refresh happened (as timestamp).
   */
  long getLastRefreshTime();

}
