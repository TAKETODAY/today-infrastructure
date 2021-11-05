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

package cn.taketoday.core.io.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffer that share
 * a memory pool. Introduces methods for reference counting.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface PooledDataBuffer extends DataBuffer {

  /**
   * Return {@code true} if this buffer is allocated;
   * {@code false} if it has been deallocated.
   */
  boolean isAllocated();

  /**
   * Increase the reference count for this buffer by one.
   *
   * @return this buffer
   */
  PooledDataBuffer retain();

  /**
   * Associate the given hint with the data buffer for debugging purposes.
   *
   * @return this buffer
   */
  PooledDataBuffer touch(Object hint);

  /**
   * Decrease the reference count for this buffer by one,
   * and deallocate it once the count reaches zero.
   *
   * @return {@code true} if the buffer was deallocated;
   * {@code false} otherwise
   */
  boolean release();

}
