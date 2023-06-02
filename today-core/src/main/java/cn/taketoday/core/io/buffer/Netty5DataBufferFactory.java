/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.CompositeBuffer;
import io.netty5.buffer.DefaultBufferAllocators;

/**
 * Implementation of the {@code DataBufferFactory} interface based on a
 * Netty 5 {@link BufferAllocator}.
 *
 * @author Violeta Georgieva
 * @author Arjen Poutsma
 * @since 4.0
 */
public class Netty5DataBufferFactory implements DataBufferFactory {

  private final BufferAllocator bufferAllocator;

  /**
   * Create a new {@code Netty5DataBufferFactory} based on the given factory.
   *
   * @param bufferAllocator the factory to use
   */
  public Netty5DataBufferFactory(BufferAllocator bufferAllocator) {
    Assert.notNull(bufferAllocator, "BufferAllocator must not be null");
    this.bufferAllocator = bufferAllocator;
  }

  /**
   * Return the {@code BufferAllocator} used by this factory.
   */
  public BufferAllocator getBufferAllocator() {
    return this.bufferAllocator;
  }

  @Override
  @Deprecated
  public Netty5DataBuffer allocateBuffer() {
    Buffer buffer = this.bufferAllocator.allocate(256);
    return new Netty5DataBuffer(buffer, this);
  }

  @Override
  public Netty5DataBuffer allocateBuffer(int initialCapacity) {
    Buffer buffer = this.bufferAllocator.allocate(initialCapacity);
    return new Netty5DataBuffer(buffer, this);
  }

  @Override
  public Netty5DataBuffer wrap(ByteBuffer byteBuffer) {
    Buffer buffer = this.bufferAllocator.copyOf(byteBuffer);
    return new Netty5DataBuffer(buffer, this);
  }

  @Override
  public Netty5DataBuffer wrap(byte[] bytes) {
    Buffer buffer = this.bufferAllocator.copyOf(bytes);
    return new Netty5DataBuffer(buffer, this);
  }

  /**
   * Wrap the given Netty {@link Buffer} in a {@code Netty5DataBuffer}.
   *
   * @param buffer the Netty buffer to wrap
   * @return the wrapped buffer
   */
  public Netty5DataBuffer wrap(Buffer buffer) {
    buffer.touch("Wrap buffer");
    return new Netty5DataBuffer(buffer, this);
  }

  /**
   * {@inheritDoc}
   * <p>This implementation uses Netty's {@link CompositeBuffer}.
   */
  @Override
  public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
    Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
    if (dataBuffers.size() == 1) {
      return dataBuffers.get(0);
    }
    CompositeBuffer composite = this.bufferAllocator.compose();
    for (DataBuffer dataBuffer : dataBuffers) {
      Assert.isInstanceOf(Netty5DataBuffer.class, dataBuffer);
      composite.extendWith(((Netty5DataBuffer) dataBuffer).getNativeBuffer().send());
    }
    return new Netty5DataBuffer(composite, this);
  }

  @Override
  public boolean isDirect() {
    return this.bufferAllocator.getAllocationType().isDirect();
  }

  /**
   * Return the given Netty {@link DataBuffer} as a {@link Buffer}.
   * <p>Returns the {@linkplain Netty5DataBuffer#getNativeBuffer() native buffer}
   * if {@code buffer} is a {@link Netty5DataBuffer}; returns
   * {@link BufferAllocator#copyOf(ByteBuffer)} otherwise.
   *
   * @param buffer the {@code DataBuffer} to return a {@code Buffer} for
   * @return the netty {@code Buffer}
   */
  public static Buffer toBuffer(DataBuffer buffer) {
    if (buffer instanceof Netty5DataBuffer netty5DataBuffer) {
      return netty5DataBuffer.getNativeBuffer();
    }
    else {
      ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.readableByteCount());
      buffer.toByteBuffer(byteBuffer);
      return DefaultBufferAllocators.preferredAllocator().copyOf(byteBuffer);
    }
  }

  @Override
  public String toString() {
    return "Netty5DataBufferFactory (" + this.bufferAllocator + ")";
  }
}
