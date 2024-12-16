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
import java.nio.charset.Charset;
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
public class NettyDataBufferFactory extends DataBufferFactory {

  private final ByteBufAllocator allocator;

  /**
   * Create a new {@code NettyDataBufferFactory} based on the given factory.
   *
   * @param allocator the factory to use
   * @see io.netty.buffer.PooledByteBufAllocator
   * @see io.netty.buffer.UnpooledByteBufAllocator
   */
  public NettyDataBufferFactory(ByteBufAllocator allocator) {
    Assert.notNull(allocator, "ByteBufAllocator is required");
    this.allocator = allocator;
  }

  /**
   * Return the {@code ByteBufAllocator} used by this factory.
   */
  public ByteBufAllocator getByteBufAllocator() {
    return this.allocator;
  }

  @Override
  public NettyDataBuffer allocateBuffer() {
    ByteBuf byteBuf = this.allocator.buffer();
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public NettyDataBuffer allocateBuffer(int initialCapacity) {
    ByteBuf byteBuf = this.allocator.buffer(initialCapacity);
    return new NettyDataBuffer(byteBuf, this);
  }

  @Override
  public DataBuffer copiedBuffer(CharSequence string, Charset charset) {
    ByteBuf byteBuf = Unpooled.copiedBuffer(string, charset);
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
    CompositeByteBuf composite = this.allocator.compositeBuffer(bufferCount);
    for (DataBuffer dataBuffer : dataBuffers) {
      Assert.isInstanceOf(NettyDataBuffer.class, dataBuffer);
      composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
    }
    return new NettyDataBuffer(composite, this);
  }

  /**
   * {@inheritDoc}
   * <p>This implementation uses Netty's {@link CompositeByteBuf}.
   */
  @Override
  public DataBuffer join(DataBuffer... dataBuffers) {
    Assert.notEmpty(dataBuffers, "DataBuffer array must not be empty");
    int bufferCount = dataBuffers.length;
    if (bufferCount == 1) {
      return dataBuffers[0];
    }
    CompositeByteBuf composite = this.allocator.compositeBuffer(bufferCount);
    for (DataBuffer dataBuffer : dataBuffers) {
      Assert.isInstanceOf(NettyDataBuffer.class, dataBuffer);
      composite.addComponent(true, ((NettyDataBuffer) dataBuffer).getNativeBuffer());
    }
    return new NettyDataBuffer(composite, this);
  }

  @Override
  public boolean isDirect() {
    return this.allocator.isDirectBufferPooled();
  }

  @Override
  public String toString() {
    return "NettyDataBufferFactory (%s)".formatted(this.allocator);
  }

}
