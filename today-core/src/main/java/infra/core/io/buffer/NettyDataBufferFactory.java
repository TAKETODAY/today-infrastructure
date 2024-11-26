/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

import infra.lang.Assert;
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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
    Assert.notNull(byteBufAllocator, "ByteBufAllocator is required");
    this.byteBufAllocator = byteBufAllocator;
  }

  /**
   * Return the {@code ByteBufAllocator} used by this factory.
   */
  public ByteBufAllocator getByteBufAllocator() {
    return this.byteBufAllocator;
  }

  @Override
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

  @Override
  public DataBuffer wrap(byte[] bytes, int offset, int length) {
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes, offset, length);
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
   * if {@code dataBuffer} is a {@link NettyDataBuffer}; returns
   * {@link Unpooled#wrappedBuffer(ByteBuffer)} otherwise.
   *
   * @param dataBuffer the {@code DataBuffer} to return a {@code ByteBuf} for
   * @return the netty {@code ByteBuf}
   */
  public static ByteBuf toByteBuf(DataBuffer dataBuffer) {
    if (dataBuffer instanceof NettyDataBuffer nettyDataBuffer) {
      return nettyDataBuffer.getNativeBuffer();
    }
    else {
      ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
      dataBuffer.toByteBuffer(byteBuffer);
      return Unpooled.wrappedBuffer(byteBuffer);
    }
  }

  @Override
  public String toString() {
    return "NettyDataBufferFactory (" + this.byteBufAllocator + ")";
  }

}
