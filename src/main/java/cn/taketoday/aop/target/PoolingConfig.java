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
 * Config interface for a pooling target source.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface PoolingConfig {

  /**
   * Return the maximum size of the pool.
   */
  int getMaxSize();

  /**
   * Return the number of active objects in the pool.
   *
   * @throws UnsupportedOperationException if not supported by the pool
   */
  int getActiveCount() throws UnsupportedOperationException;

  /**
   * Return the number of idle objects in the pool.
   *
   * @throws UnsupportedOperationException if not supported by the pool
   */
  int getIdleCount() throws UnsupportedOperationException;

}
