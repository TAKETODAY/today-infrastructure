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
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import infra.lang.Assert;
import infra.lang.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import static infra.core.io.buffer.DataBufferUtils.logger;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty
 * {@link ByteBuf}. Typically constructed with {@link NettyDataBufferFactory}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NettyDataBuffer extends DataBuffer {

  private ByteBuf byteBuf;

  private final NettyDataBufferFactory dataBufferFactory;

  /**
   * Create a new {@code NettyDataBuffer} based on the given {@code ByteBuff}.
   *
   * @param byteBuf the buffer to base this buffer on
   */
  NettyDataBuffer(ByteBuf byteBuf, NettyDataBufferFactory dataBufferFactory) {
    Assert.notNull(byteBuf, "ByteBuf is required");
    Assert.notNull(dataBufferFactory, "NettyDataBufferFactory is required");
    this.byteBuf = byteBuf;
    this.dataBufferFactory = dataBufferFactory;
  }

  /**
   * Directly exposes the native {@code ByteBuf} that this buffer is based on.
   *
   * @return the wrapped byte buffer
   */
  public ByteBuf getNativeBuffer() {
    return this.byteBuf;
  }

  /**
   * Return the given Netty {@link DataBuffer} as a {@link ByteBuf}.
   * <p>Returns the {@linkplain NettyDataBuffer#getNativeBuffer() native buffer}
   * if {@code dataBuffer} is a {@link NettyDataBuffer}; returns
   * {@link Unpooled#wrappedBuffer(ByteBuffer)} otherwise.
   *
   * @param dataBuffer the {@code DataBuffer} to return a {@code ByteBuf} for
   * @return the netty {@code ByteBuf}
   * @since 5.0
   */
  public static ByteBuf toByteBuf(DataBuffer dataBuffer) {
    if (dataBuffer instanceof NettyDataBuffer nettyDataBuffer) {
      return nettyDataBuffer.getNativeBuffer();
    }
    else if (dataBuffer.readableBytes() == 0) {
      return Unpooled.EMPTY_BUFFER;
    }
    else {
      ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableBytes());
      dataBuffer.toByteBuffer(byteBuffer);
      return Unpooled.wrappedBuffer(byteBuffer);
    }
  }

  @Override
  public NettyDataBufferFactory factory() {
    return this.dataBufferFactory;
  }

  @Override
  public int indexOf(IntPredicate predicate, int fromIndex) {
    Assert.notNull(predicate, "IntPredicate is required");
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    else if (fromIndex >= this.byteBuf.writerIndex()) {
      return -1;
    }
    int length = this.byteBuf.writerIndex() - fromIndex;
    return this.byteBuf.forEachByte(fromIndex, length, predicate.negate()::test);
  }

  @Override
  public int lastIndexOf(IntPredicate predicate, int fromIndex) {
    Assert.notNull(predicate, "IntPredicate is required");
    if (fromIndex < 0) {
      return -1;
    }
    fromIndex = Math.min(fromIndex, this.byteBuf.writerIndex() - 1);
    return this.byteBuf.forEachByteDesc(0, fromIndex + 1, predicate.negate()::test);
  }

  @Override
  public int readableBytes() {
    return this.byteBuf.readableBytes();
  }

  @Override
  public int writableBytes() {
    return this.byteBuf.writableBytes();
  }

  @Override
  public int readPosition() {
    return this.byteBuf.readerIndex();
  }

  @Override
  public NettyDataBuffer readPosition(int readPosition) {
    this.byteBuf.readerIndex(readPosition);
    return this;
  }

  @Override
  public int writePosition() {
    return this.byteBuf.writerIndex();
  }

  @Override
  public NettyDataBuffer writePosition(int writePosition) {
    this.byteBuf.writerIndex(writePosition);
    return this;
  }

  @Override
  public byte getByte(int index) {
    return this.byteBuf.getByte(index);
  }

  @Override
  public int capacity() {
    return this.byteBuf.capacity();
  }

  @Override
  public NettyDataBuffer capacity(int capacity) {
    this.byteBuf.capacity(capacity);
    return this;
  }

  @Override
  public DataBuffer ensureWritable(int capacity) {
    this.byteBuf.ensureWritable(capacity);
    return this;
  }

  @Override
  public byte read() {
    return this.byteBuf.readByte();
  }

  @Override
  public NettyDataBuffer read(byte[] destination) {
    this.byteBuf.readBytes(destination);
    return this;
  }

  @Override
  public NettyDataBuffer read(byte[] destination, int offset, int length) {
    this.byteBuf.readBytes(destination, offset, length);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte b) {
    this.byteBuf.writeByte(b);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte[] source) {
    this.byteBuf.writeBytes(source);
    return this;
  }

  @Override
  public NettyDataBuffer write(byte[] source, int offset, int length) {
    this.byteBuf.writeBytes(source, offset, length);
    return this;
  }

  @Override
  public DataBuffer write(@Nullable ByteBuffer source) {
    if (source != null) {
      byteBuf.writeBytes(source);
    }
    return this;
  }

  @Override
  public DataBuffer write(@Nullable DataBuffer source) {
    if (source instanceof NettyDataBuffer ndb) {
      byteBuf.writeBytes(ndb.byteBuf);
    }
    else if (source != null) {
      byteBuf.writeBytes(source.toByteBuffer());
    }
    return this;
  }

  @Override
  public DataBuffer write(CharSequence charSequence, Charset charset) {
    Assert.notNull(charSequence, "CharSequence is required");
    Assert.notNull(charset, "Charset is required");
    if (StandardCharsets.UTF_8.equals(charset)) {
      ByteBufUtil.writeUtf8(this.byteBuf, charSequence);
    }
    else if (StandardCharsets.US_ASCII.equals(charset)) {
      ByteBufUtil.writeAscii(this.byteBuf, charSequence);
    }
    else {
      return super.write(charSequence, charset);
    }
    return this;
  }

  @Override
  public NettyDataBuffer slice(int index, int length) {
    ByteBuf slice = this.byteBuf.slice(index, length);
    return new NettyDataBuffer(slice, this.dataBufferFactory);
  }

  @Override
  public NettyDataBuffer retainedSlice(int index, int length) {
    ByteBuf slice = this.byteBuf.retainedSlice(index, length);
    return new NettyDataBuffer(slice, this.dataBufferFactory);
  }

  @Override
  public NettyDataBuffer split(int index) {
    ByteBuf split = this.byteBuf.retainedSlice(0, index);
    int writerIndex = this.byteBuf.writerIndex();
    int readerIndex = this.byteBuf.readerIndex();

    split.writerIndex(Math.min(writerIndex, index));
    split.readerIndex(Math.min(readerIndex, index));

    this.byteBuf = this.byteBuf.slice(index, this.byteBuf.capacity() - index);
    this.byteBuf.writerIndex(Math.max(writerIndex, index) - index);
    this.byteBuf.readerIndex(Math.max(readerIndex, index) - index);

    return new NettyDataBuffer(split, this.dataBufferFactory);
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return this.byteBuf.nioBuffer();
  }

  @Override
  public ByteBuffer asByteBuffer(int index, int length) {
    return this.byteBuf.nioBuffer(index, length);
  }

  @Override
  public ByteBuffer toByteBuffer(int index, int length) {
    ByteBuffer result = this.byteBuf.isDirect()
            ? ByteBuffer.allocateDirect(length)
            : ByteBuffer.allocate(length);

    this.byteBuf.getBytes(index, result);

    return result.flip();
  }

  @Override
  public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
    Assert.notNull(dest, "Dest is required");

    dest = dest.duplicate().clear();
    dest.put(destPos, this.byteBuf.nioBuffer(srcPos, length), 0, length);
  }

  @Override
  public ByteBufferIterator readableByteBuffers() {
    ByteBuffer[] readable = this.byteBuf.nioBuffers(this.byteBuf.readerIndex(), this.byteBuf.readableBytes());
    return new ByteBufferIterator(readable, true);
  }

  @Override
  public ByteBufferIterator writableByteBuffers() {
    ByteBuffer[] writable = this.byteBuf.nioBuffers(this.byteBuf.writerIndex(), this.byteBuf.writableBytes());
    return new ByteBufferIterator(writable, false);
  }

  @Override
  public String toString(Charset charset) {
    Assert.notNull(charset, "Charset is required");
    return this.byteBuf.toString(charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    Assert.notNull(charset, "Charset is required");
    return this.byteBuf.toString(index, length, charset);
  }

  @Override
  public boolean isAllocated() {
    return this.byteBuf.refCnt() > 0;
  }

  @Override
  public NettyDataBuffer retain() {
    this.byteBuf = byteBuf.retain();
    return this;
  }

  @Override
  public NettyDataBuffer touch(Object hint) {
    this.byteBuf.touch(hint);
    return this;
  }

  @Override
  public boolean release() {
    try {
      return byteBuf.release();
    }
    catch (IllegalStateException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Failed to release DataBuffer: {}", this, ex);
      }
    }
    return false;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other || (other instanceof NettyDataBuffer
            && this.byteBuf.equals(((NettyDataBuffer) other).byteBuf));
  }

  @Override
  public int hashCode() {
    return this.byteBuf.hashCode();
  }

  @Override
  public String toString() {
    return this.byteBuf.toString();
  }

  private static final class ByteBufferIterator implements DataBuffer.ByteBufferIterator {

    private final ByteBuffer[] byteBuffers;

    private final boolean readOnly;

    private int cursor = 0;

    public ByteBufferIterator(ByteBuffer[] byteBuffers, boolean readOnly) {
      this.byteBuffers = byteBuffers;
      this.readOnly = readOnly;
    }

    @Override
    public boolean hasNext() {
      return this.cursor < this.byteBuffers.length;
    }

    @Override
    public ByteBuffer next() {
      int index = this.cursor;
      if (index < this.byteBuffers.length) {
        this.cursor = index + 1;
        ByteBuffer next = this.byteBuffers[index];
        return this.readOnly ? next.asReadOnlyBuffer() : next;
      }
      else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void close() { }
  }

}
