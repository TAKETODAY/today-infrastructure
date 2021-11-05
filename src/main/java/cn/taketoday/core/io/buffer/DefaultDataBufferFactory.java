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

import cn.taketoday.lang.Assert;

/**
 * Default implementation of the {@code DataBufferFactory} interface. Allows for
 * specification of the default initial capacity at construction time, as well
 * as whether heap-based or direct buffers are to be preferred.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class DefaultDataBufferFactory implements DataBufferFactory {

  /**
   * The default capacity when none is specified.
   *
   * @see #DefaultDataBufferFactory()
   * @see #DefaultDataBufferFactory(boolean)
   */
  public static final int DEFAULT_INITIAL_CAPACITY = 256;

  /**
   * Shared instance based on the default constructor.
   */
  public static final DefaultDataBufferFactory sharedInstance = new DefaultDataBufferFactory();

  private final boolean preferDirect;

  private final int defaultInitialCapacity;

  /**
   * Creates a new {@code DefaultDataBufferFactory} with default settings.
   *
   * @see #sharedInstance
   */
  public DefaultDataBufferFactory() {
    this(false);
  }

  /**
   * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
   * buffers should be created by {@link #allocateBuffer()} and
   * {@link #allocateBuffer(int)}.
   *
   * @param preferDirect {@code true} if direct buffers are to be preferred;
   * {@code false} otherwise
   */
  public DefaultDataBufferFactory(boolean preferDirect) {
    this(preferDirect, DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Creates a new {@code DefaultDataBufferFactory}, indicating whether direct
   * buffers should be created by {@link #allocateBuffer()} and
   * {@link #allocateBuffer(int)}, and what the capacity is to be used for
   * {@link #allocateBuffer()}.
   *
   * @param preferDirect {@code true} if direct buffers are to be preferred;
   * {@code false} otherwise
   */
  public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
    Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");
    this.preferDirect = preferDirect;
    this.defaultInitialCapacity = defaultInitialCapacity;
  }

  @Override
  public DefaultDataBuffer allocateBuffer() {
    return allocateBuffer(this.defaultInitialCapacity);
  }

  @Override
  public DefaultDataBuffer allocateBuffer(int initialCapacity) {
    ByteBuffer byteBuffer = this.preferDirect
                            ? ByteBuffer.allocateDirect(initialCapacity)
                            : ByteBuffer.allocate(initialCapacity);
    return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
  }

  @Override
  public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
    return DefaultDataBuffer.fromFilledByteBuffer(this, byteBuffer.slice());
  }

  @Override
  public DefaultDataBuffer wrap(byte[] bytes) {
    return DefaultDataBuffer.fromFilledByteBuffer(this, ByteBuffer.wrap(bytes));
  }

  /**
   * {@inheritDoc}
   * <p>This implementation creates a single {@link DefaultDataBuffer}
   * to contain the data in {@code dataBuffers}.
   */
  @Override
  public DefaultDataBuffer join(List<? extends DataBuffer> dataBuffers) {
    Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
    int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
    DefaultDataBuffer result = allocateBuffer(capacity);
    dataBuffers.forEach(result::write);
    dataBuffers.forEach(DataBufferUtils::release);
    return result;
  }

  @Override
  public String toString() {
    return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
  }

}
