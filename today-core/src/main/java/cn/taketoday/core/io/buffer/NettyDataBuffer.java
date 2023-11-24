/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty
 * {@link ByteBuf}. Typically constructed with {@link NettyDataBufferFactory}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NettyDataBuffer implements PooledDataBuffer {

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
  public int readableByteCount() {
    return this.byteBuf.readableBytes();
  }

  @Override
  public int writableByteCount() {
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
  public NettyDataBuffer write(DataBuffer... dataBuffers) {
    if (ObjectUtils.isNotEmpty(dataBuffers)) {
      if (hasNettyDataBuffers(dataBuffers)) {
        ByteBuf[] nativeBuffers = new ByteBuf[dataBuffers.length];
        for (int i = 0; i < dataBuffers.length; i++) {
          nativeBuffers[i] = ((NettyDataBuffer) dataBuffers[i]).getNativeBuffer();
        }
        write(nativeBuffers);
      }
      else {
        ByteBuffer[] byteBuffers = new ByteBuffer[dataBuffers.length];
        for (int i = 0; i < dataBuffers.length; i++) {
          byteBuffers[i] = ByteBuffer.allocate(dataBuffers[i].readableByteCount());
          dataBuffers[i].toByteBuffer(byteBuffers[i]);
        }
        write(byteBuffers);
      }
    }
    return this;
  }

  private static boolean hasNettyDataBuffers(DataBuffer[] buffers) {
    for (DataBuffer buffer : buffers) {
      if (!(buffer instanceof NettyDataBuffer)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public NettyDataBuffer write(ByteBuffer... buffers) {
    if (ObjectUtils.isNotEmpty(buffers)) {
      for (ByteBuffer buffer : buffers) {
        this.byteBuf.writeBytes(buffer);
      }
    }
    return this;
  }

  /**
   * Writes one or more Netty {@link ByteBuf ByteBufs} to this buffer,
   * starting at the current writing position.
   *
   * @param byteBufs the buffers to write into this buffer
   * @return this buffer
   */
  public NettyDataBuffer write(ByteBuf... byteBufs) {
    if (ObjectUtils.isNotEmpty(byteBufs)) {
      for (ByteBuf byteBuf : byteBufs) {
        this.byteBuf.writeBytes(byteBuf);
      }
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
      return PooledDataBuffer.super.write(charSequence, charset);
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
  public DataBuffer.ByteBufferIterator readableByteBuffers() {
    ByteBuffer[] readable = this.byteBuf.nioBuffers(this.byteBuf.readerIndex(), this.byteBuf.readableBytes());
    return new ByteBufferIterator(readable, true);
  }

  @Override
  public DataBuffer.ByteBufferIterator writableByteBuffers() {
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
  public PooledDataBuffer retain() {
    return new NettyDataBuffer(this.byteBuf.retain(), this.dataBufferFactory);
  }

  @Override
  public PooledDataBuffer touch(Object hint) {
    this.byteBuf.touch(hint);
    return this;
  }

  @Override
  public boolean release() {
    return this.byteBuf.release();
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
