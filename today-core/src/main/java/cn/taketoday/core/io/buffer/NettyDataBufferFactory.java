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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Implementation of the {@code DataBufferFactory} interface based on a
 * Netty {@link ByteBufAllocator}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see io.netty.buffer.PooledByteBufAllocator
 * @see io.netty.buffer.UnpooledByteBufAllocator
 * @since 4.0
 */
public class NettyDataBufferFactory implements DataBufferFactory {

  private final ByteBufAllocator byteBufAllocator;

  /**
   * Create a new {@code NettyDataBufferFactory} based on the given factory.
   *
   * @param byteBufAllocator the factory to use
   * @see io.netty.buffer.PooledByteBufAllocator
   * @see io.netty.buffer.UnpooledByteBufAllocator
   */
  public NettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
    Assert.notNull(byteBufAllocator, "ByteBufAllocator must not be null");
    this.byteBufAllocator = byteBufAllocator;
  }

  /**
   * Return the {@code ByteBufAllocator} used by this factory.
   */
  public ByteBufAllocator getByteBufAllocator() {
    return this.byteBufAllocator;
  }

  @Override
  @Deprecated
  public NettyDataBuffer allocateBuffer() {
    ByteBuf byteBuf = this.byteBufAllocator.buffer();
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public NettyDataBuffer allocateBuffer(int initialCapacity) {
    ByteBuf byteBuf = this.byteBufAllocator.buffer(initialCapacity);
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public NettyDataBuffer wrap(ByteBuffer byteBuffer) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(byteBuffer);
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public DataBuffer wrap(byte[] bytes) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    return new NettyDataBuffer(byteBuf, this);
  }

  /**
   * Wrap the given Netty {@link ByteBuf} in a {@code NettyDataBuffer}.
   *
   * @param byteBuf the Netty byte buffer to wrap
   * @return the wrapped buffer
   */
  public NettyDataBuffer wrap(ByteBuf byteBuf) {
    byteBuf.touch();
    return new NettyDataBuffer(byteBuf, this);
  }

  /**
   * {@inheritDoc}
   * <p>This implementation uses Netty's {@link CompositeByteBuf}.
   */
  @Override
  public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
    Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
    int bufferCount = dataBuffers.size();
    if (bufferCount == 1) {
      return dataBuffers.get(0);
    }
    CompositeByteBuf composite = this.byteBufAllocator.compositeBuffer(bufferCount);
    for (DataBuffer dataBuffer : dataBuffers) {
      Assert.isInstanceOf(NettyDataBuffer.class, dataBuffer);
      composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
    }
    return new NettyDataBuffer(composite, this);
  }

  @Override
  public boolean isDirect() {
    return this.byteBufAllocator.isDirectBufferPooled();
  }

  /**
   * Return the given Netty {@link DataBuffer} as a {@link ByteBuf}.
   * <p>Returns the {@linkplain NettyDataBuffer#getNativeBuffer() native buffer}
   * if {@code buffer} is a {@link NettyDataBuffer}; returns
   * {@link Unpooled#wrappedBuffer(ByteBuffer)} otherwise.
   *
   * @param buffer the {@code DataBuffer} to return a {@code ByteBuf} for
   * @return the netty {@code ByteBuf}
   */
  public static ByteBuf toByteBuf(DataBuffer buffer) {
    if (buffer instanceof NettyDataBuffer nettyDataBuffer) {
      return nettyDataBuffer.getNativeBuffer();
    }
    else {
      return Unpooled.wrappedBuffer(buffer.toByteBuffer());
    }
  }

  @Override
  public String toString() {
    return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
  }

}
