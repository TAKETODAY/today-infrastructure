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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A factory for {@link DataBuffer DataBuffers}, allowing for allocation and
 * wrapping of data buffers.
 *
 * @author Arjen Poutsma
 * @see DataBuffer
 * @since 4.0
 */
public interface DataBufferFactory {

  /**
   * Allocate a data buffer of a default initial capacity. Depending on the
   * underlying implementation and its configuration, this will be heap-based
   * or direct buffer.
   *
   * @return the allocated buffer
   */
  DataBuffer allocateBuffer();

  /**
   * Allocate a data buffer of the given initial capacity. Depending on the
   * underlying implementation and its configuration, this will be heap-based
   * or direct buffer.
   *
   * @param initialCapacity the initial capacity of the buffer to allocate
   * @return the allocated buffer
   */
  DataBuffer allocateBuffer(int initialCapacity);

  /**
   * Wrap the given {@link ByteBuffer} in a {@code DataBuffer}. Unlike
   * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
   *
   * @param byteBuffer the NIO byte buffer to wrap
   * @return the wrapped buffer
   */
  DataBuffer wrap(ByteBuffer byteBuffer);

  /**
   * Wrap the given {@code byte} array in a {@code DataBuffer}. Unlike
   * {@linkplain #allocateBuffer(int) allocating}, wrapping does not use new memory.
   *
   * @param bytes the byte array to wrap
   * @return the wrapped buffer
   */
  DataBuffer wrap(byte[] bytes);

  /**
   * Return a new {@code DataBuffer} composed of the {@code dataBuffers} elements joined together.
   * Depending on the implementation, the returned buffer may be a single buffer containing all
   * data of the provided buffers, or it may be a true composite that contains references to the
   * buffers.
   * <p>Note that the given data buffers do <strong>not</strong> have to be released, as they are
   * released as part of the returned composite.
   *
   * @param dataBuffers the data buffers to be composed
   * @return a buffer that is composed of the {@code dataBuffers} argument
   */
  DataBuffer join(List<? extends DataBuffer> dataBuffers);

  /**
   * Indicates whether this factory allocates direct buffers (i.e. non-heap,
   * native memory).
   *
   * @return {@code true} if this factory allocates direct buffers;
   * {@code false} otherwise
   */
  boolean isDirect();

}
