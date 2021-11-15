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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.IntPredicate;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Default implementation of the {@link DataBuffer} interface that uses a
 * {@link ByteBuffer} internally. with separate read and write positions.
 * Constructed using the {@link DefaultDataBufferFactory}.
 *
 * <p>Inspired by Netty's {@code ByteBuf}. Introduced so that non-Netty runtimes
 * (i.e. Servlet) do not require Netty on the classpath.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @see DefaultDataBufferFactory
 * @since 4.0
 */
public class DefaultDataBuffer implements DataBuffer {

  private static final int MAX_CAPACITY = Integer.MAX_VALUE;

  private static final int CAPACITY_THRESHOLD = 1024 * 1024 * 4;

  private final DefaultDataBufferFactory dataBufferFactory;

  private ByteBuffer byteBuffer;

  private int capacity;

  private int readPosition;

  private int writePosition;

  private DefaultDataBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
    Assert.notNull(dataBufferFactory, "DefaultDataBufferFactory must not be null");
    Assert.notNull(byteBuffer, "ByteBuffer must not be null");
    this.dataBufferFactory = dataBufferFactory;
    ByteBuffer slice = byteBuffer.slice();
    this.byteBuffer = slice;
    this.capacity = slice.remaining();
  }

  static DefaultDataBuffer fromFilledByteBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
    DefaultDataBuffer dataBuffer = new DefaultDataBuffer(dataBufferFactory, byteBuffer);
    dataBuffer.writePosition(byteBuffer.remaining());
    return dataBuffer;
  }

  static DefaultDataBuffer fromEmptyByteBuffer(DefaultDataBufferFactory dataBufferFactory, ByteBuffer byteBuffer) {
    return new DefaultDataBuffer(dataBufferFactory, byteBuffer);
  }

  /**
   * Directly exposes the native {@code ByteBuffer} that this buffer is based
   * on also updating the {@code ByteBuffer's} position and limit to match
   * the current {@link #readPosition()} and {@link #readableByteCount()}.
   *
   * @return the wrapped byte buffer
   */
  public ByteBuffer getNativeBuffer() {
    this.byteBuffer.position(this.readPosition);
    this.byteBuffer.limit(readableByteCount());
    return this.byteBuffer;
  }

  private void setNativeBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
    this.capacity = byteBuffer.remaining();
  }

  @Override
  public DefaultDataBufferFactory factory() {
    return this.dataBufferFactory;
  }

  @Override
  public int indexOf(IntPredicate predicate, int fromIndex) {
    Assert.notNull(predicate, "IntPredicate must not be null");
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    else if (fromIndex >= this.writePosition) {
      return -1;
    }
    for (int i = fromIndex; i < this.writePosition; i++) {
      byte b = this.byteBuffer.get(i);
      if (predicate.test(b)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(IntPredicate predicate, int fromIndex) {
    Assert.notNull(predicate, "IntPredicate must not be null");
    int i = Math.min(fromIndex, this.writePosition - 1);
    for (; i >= 0; i--) {
      byte b = this.byteBuffer.get(i);
      if (predicate.test(b)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int readableByteCount() {
    return this.writePosition - this.readPosition;
  }

  @Override
  public int writableByteCount() {
    return this.capacity - this.writePosition;
  }

  @Override
  public int readPosition() {
    return this.readPosition;
  }

  @Override
  public DefaultDataBuffer readPosition(int readPosition) {
    assertIndex(readPosition >= 0, "'readPosition' %d must be >= 0", readPosition);
    assertIndex(readPosition <= this.writePosition, "'readPosition' %d must be <= %d",
                readPosition, this.writePosition);
    this.readPosition = readPosition;
    return this;
  }

  @Override
  public int writePosition() {
    return this.writePosition;
  }

  @Override
  public DefaultDataBuffer writePosition(int writePosition) {
    assertIndex(writePosition >= this.readPosition, "'writePosition' %d must be >= %d",
                writePosition, this.readPosition);
    assertIndex(writePosition <= this.capacity, "'writePosition' %d must be <= %d",
                writePosition, this.capacity);
    this.writePosition = writePosition;
    return this;
  }

  @Override
  public int capacity() {
    return this.capacity;
  }

  @Override
  public DefaultDataBuffer capacity(int newCapacity) {
    if (newCapacity <= 0) {
      throw new IllegalArgumentException(String.format("'newCapacity' %d must be higher than 0", newCapacity));
    }
    int readPosition = readPosition();
    int writePosition = writePosition();
    int oldCapacity = capacity();

    if (newCapacity > oldCapacity) {
      ByteBuffer oldBuffer = this.byteBuffer;
      ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());
      oldBuffer.position(0).limit(oldBuffer.capacity());
      newBuffer.position(0).limit(oldBuffer.capacity());
      newBuffer.put(oldBuffer);
      newBuffer.clear();
      setNativeBuffer(newBuffer);
    }
    else if (newCapacity < oldCapacity) {
      ByteBuffer oldBuffer = this.byteBuffer;
      ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());
      if (readPosition < newCapacity) {
        if (writePosition > newCapacity) {
          writePosition = newCapacity;
          writePosition(writePosition);
        }
        oldBuffer.position(readPosition).limit(writePosition);
        newBuffer.position(readPosition).limit(writePosition);
        newBuffer.put(oldBuffer);
        newBuffer.clear();
      }
      else {
        readPosition(newCapacity);
        writePosition(newCapacity);
      }
      setNativeBuffer(newBuffer);
    }
    return this;
  }

  @Override
  public DataBuffer ensureCapacity(int length) {
    if (length > writableByteCount()) {
      int newCapacity = calculateCapacity(this.writePosition + length);
      capacity(newCapacity);
    }
    return this;
  }

  private static ByteBuffer allocate(int capacity, boolean direct) {
    return (direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
  }

  @Override
  public byte getByte(int index) {
    assertIndex(index >= 0, "index %d must be >= 0", index);
    assertIndex(index <= this.writePosition - 1, "index %d must be <= %d", index, this.writePosition - 1);
    return this.byteBuffer.get(index);
  }

  @Override
  public byte read() {
    assertIndex(this.readPosition <= this.writePosition - 1, "readPosition %d must be <= %d",
                this.readPosition, this.writePosition - 1);
    int pos = this.readPosition;
    byte b = this.byteBuffer.get(pos);
    this.readPosition = pos + 1;
    return b;
  }

  @Override
  public DefaultDataBuffer read(byte[] destination) {
    Assert.notNull(destination, "Byte array must not be null");
    read(destination, 0, destination.length);
    return this;
  }

  @Override
  public DefaultDataBuffer read(byte[] destination, int offset, int length) {
    Assert.notNull(destination, "Byte array must not be null");
    assertIndex(this.readPosition <= this.writePosition - length,
                "readPosition %d and length %d should be smaller than writePosition %d",
                this.readPosition, length, this.writePosition);

    ByteBuffer tmp = this.byteBuffer.duplicate();
    int limit = this.readPosition + length;
    tmp.clear().position(this.readPosition).limit(limit);
    tmp.get(destination, offset, length);

    this.readPosition += length;
    return this;
  }

  @Override
  public DefaultDataBuffer write(byte b) {
    ensureCapacity(1);
    int pos = this.writePosition;
    this.byteBuffer.put(pos, b);
    this.writePosition = pos + 1;
    return this;
  }

  @Override
  public DefaultDataBuffer write(byte[] source) {
    Assert.notNull(source, "Byte array must not be null");
    write(source, 0, source.length);
    return this;
  }

  @Override
  public DefaultDataBuffer write(byte[] source, int offset, int length) {
    Assert.notNull(source, "Byte array must not be null");
    ensureCapacity(length);

    ByteBuffer tmp = this.byteBuffer.duplicate();
    int limit = this.writePosition + length;
    tmp.clear().position(this.writePosition).limit(limit);
    tmp.put(source, offset, length);

    this.writePosition += length;
    return this;
  }

  @Override
  public DefaultDataBuffer write(DataBuffer... buffers) {
    if (ObjectUtils.isNotEmpty(buffers)) {
      write(Arrays.stream(buffers)
                    .map(DataBuffer::asByteBuffer)
                    .toArray(ByteBuffer[]::new)
      );
    }
    return this;
  }

  @Override
  public DefaultDataBuffer write(ByteBuffer... buffers) {
    if (ObjectUtils.isNotEmpty(buffers)) {
      int capacity = Arrays.stream(buffers).mapToInt(ByteBuffer::remaining).sum();
      ensureCapacity(capacity);
      Arrays.stream(buffers).forEach(this::write);
    }
    return this;
  }

  private void write(ByteBuffer source) {
    int length = source.remaining();
    ByteBuffer tmp = this.byteBuffer.duplicate();
    int limit = this.writePosition + source.remaining();
    tmp.clear().position(this.writePosition).limit(limit);
    tmp.put(source);
    this.writePosition += length;
  }

  @Override
  public DefaultDataBuffer slice(int index, int length) {
    checkIndex(index, length);
    int oldPosition = this.byteBuffer.position();
    try {
      this.byteBuffer.position(index);
      ByteBuffer slice = this.byteBuffer.slice();
      slice.limit(length);
      return new SlicedDefaultDataBuffer(slice, this.dataBufferFactory, length);
    }
    finally {
      this.byteBuffer.position(oldPosition);
    }
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return asByteBuffer(this.readPosition, readableByteCount());
  }

  @Override
  public ByteBuffer asByteBuffer(int index, int length) {
    checkIndex(index, length);

    ByteBuffer duplicate = this.byteBuffer.duplicate();
    duplicate.position(index);
    duplicate.limit(index + length);
    return duplicate.slice();
  }

  @Override
  public InputStream asInputStream() {
    return new DefaultDataBufferInputStream();
  }

  @Override
  public InputStream asInputStream(boolean releaseOnClose) {
    return new DefaultDataBufferInputStream();
  }

  @Override
  public OutputStream asOutputStream() {
    return new DefaultDataBufferOutputStream();
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    checkIndex(index, length);
    Assert.notNull(charset, "Charset must not be null");

    byte[] bytes;
    int offset;

    if (this.byteBuffer.hasArray()) {
      bytes = this.byteBuffer.array();
      offset = this.byteBuffer.arrayOffset() + index;
    }
    else {
      bytes = new byte[length];
      offset = 0;
      ByteBuffer duplicate = this.byteBuffer.duplicate();
      duplicate.clear().position(index).limit(index + length);
      duplicate.get(bytes, 0, length);
    }
    return new String(bytes, offset, length, charset);
  }

  /**
   * Calculate the capacity of the buffer.
   *
   * @see io.netty.buffer.AbstractByteBufAllocator#calculateNewCapacity(int, int)
   */
  private int calculateCapacity(int neededCapacity) {
    Assert.isTrue(neededCapacity >= 0, "'neededCapacity' must >= 0");

    if (neededCapacity == CAPACITY_THRESHOLD) {
      return CAPACITY_THRESHOLD;
    }
    else if (neededCapacity > CAPACITY_THRESHOLD) {
      int newCapacity = neededCapacity / CAPACITY_THRESHOLD * CAPACITY_THRESHOLD;
      if (newCapacity > MAX_CAPACITY - CAPACITY_THRESHOLD) {
        newCapacity = MAX_CAPACITY;
      }
      else {
        newCapacity += CAPACITY_THRESHOLD;
      }
      return newCapacity;
    }
    else {
      int newCapacity = 64;
      while (newCapacity < neededCapacity) {
        newCapacity <<= 1;
      }
      return newCapacity;
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof DefaultDataBuffer otherBuffer) {
      return this.readPosition == otherBuffer.readPosition
              && this.writePosition == otherBuffer.writePosition
              && this.byteBuffer.equals(otherBuffer.byteBuffer);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.byteBuffer.hashCode();
  }

  @Override
  public String toString() {
    return String.format("DefaultDataBuffer (r: %d, w: %d, c: %d)",
                         this.readPosition, this.writePosition, this.capacity);
  }

  private void checkIndex(int index, int length) {
    assertIndex(index >= 0, "index %d must be >= 0", index);
    assertIndex(length >= 0, "length %d must be >= 0", length);
    assertIndex(index <= this.capacity, "index %d must be <= %d", index, this.capacity);
    assertIndex(length <= this.capacity, "length %d must be <= %d", length, this.capacity);
  }

  private void assertIndex(boolean expression, String format, Object... args) {
    if (!expression) {
      String message = String.format(format, args);
      throw new IndexOutOfBoundsException(message);
    }
  }

  private class DefaultDataBufferInputStream extends InputStream {

    @Override
    public int available() {
      return readableByteCount();
    }

    @Override
    public int read() {
      return available() > 0 ? DefaultDataBuffer.this.read() & 0xFF : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
      int available = available();
      if (available > 0) {
        len = Math.min(len, available);
        DefaultDataBuffer.this.read(bytes, off, len);
        return len;
      }
      else {
        return -1;
      }
    }
  }

  private class DefaultDataBufferOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      DefaultDataBuffer.this.write((byte) b);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
      DefaultDataBuffer.this.write(bytes, off, len);
    }
  }

  private static class SlicedDefaultDataBuffer extends DefaultDataBuffer {

    SlicedDefaultDataBuffer(ByteBuffer byteBuffer, DefaultDataBufferFactory dataBufferFactory, int length) {
      super(dataBufferFactory, byteBuffer);
      writePosition(length);
    }

    @Override
    public DefaultDataBuffer capacity(int newCapacity) {
      throw new UnsupportedOperationException("Changing the capacity of a sliced buffer is not supported");
    }
  }

}
