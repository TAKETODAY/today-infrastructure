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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.IntPredicate;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Provides a convenient implementation of the {@link DataBuffer} interface
 * that can be overridden to adapt the delegate.
 *
 * <p>These methods default to calling through to the wrapped delegate object.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class DataBufferWrapper extends DataBuffer {

  private final DataBuffer delegate;

  /**
   * Create a new {@code DataBufferWrapper} that wraps the given buffer.
   *
   * @param delegate the buffer to wrap
   */
  public DataBufferWrapper(DataBuffer delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  /**
   * Return the wrapped delegate.
   */
  public DataBuffer dataBuffer() {
    return this.delegate;
  }

  @Override
  public DataBufferFactory factory() {
    return this.delegate.factory();
  }

  @Override
  public int indexOf(IntPredicate predicate, int fromIndex) {
    return this.delegate.indexOf(predicate, fromIndex);
  }

  @Override
  public int lastIndexOf(IntPredicate predicate, int fromIndex) {
    return this.delegate.lastIndexOf(predicate, fromIndex);
  }

  @Override
  public int readableBytes() {
    return this.delegate.readableBytes();
  }

  @Override
  public int writableBytes() {
    return this.delegate.writableBytes();
  }

  @Override
  public int capacity() {
    return this.delegate.capacity();
  }

  @Override
  public DataBufferWrapper capacity(int capacity) {
    this.delegate.capacity(capacity);
    return this;
  }

  @Override
  public DataBufferWrapper ensureWritable(int capacity) {
    this.delegate.ensureWritable(capacity);
    return this;
  }

  @Override
  public int readPosition() {
    return this.delegate.readPosition();
  }

  @Override
  public DataBufferWrapper readPosition(int readPosition) {
    this.delegate.readPosition(readPosition);
    return this;
  }

  @Override
  public int writePosition() {
    return this.delegate.writePosition();
  }

  @Override
  public DataBufferWrapper writePosition(int writePosition) {
    this.delegate.writePosition(writePosition);
    return this;
  }

  @Override
  public byte getByte(int index) {
    return this.delegate.getByte(index);
  }

  @Override
  public byte read() {
    return this.delegate.read();
  }

  @Override
  public DataBufferWrapper read(byte[] destination) {
    this.delegate.read(destination);
    return this;
  }

  @Override
  public DataBufferWrapper read(byte[] destination, int offset, int length) {
    this.delegate.read(destination, offset, length);
    return this;
  }

  @Override
  public DataBufferWrapper write(byte b) {
    this.delegate.write(b);
    return this;
  }

  @Override
  public DataBufferWrapper write(byte[] source) {
    this.delegate.write(source);
    return this;
  }

  @Override
  public DataBufferWrapper write(byte[] source, int offset, int length) {
    this.delegate.write(source, offset, length);
    return this;
  }

  @Override
  public DataBufferWrapper write(DataBuffer... buffers) {
    this.delegate.write(buffers);
    return this;
  }

  @Override
  public DataBufferWrapper write(ByteBuffer... buffers) {
    this.delegate.write(buffers);
    return this;
  }

  @Override
  public DataBufferWrapper write(@Nullable DataBuffer source) {
    delegate.write(source);
    return this;
  }

  @Override
  public DataBufferWrapper write(@Nullable ByteBuffer source) {
    delegate.write(source);
    return this;
  }

  @Override
  public DataBufferWrapper write(CharSequence charSequence, Charset charset) {
    delegate.write(charSequence, charset);
    return this;
  }

  @Override
  public DataBuffer slice(int index, int length) {
    return this.delegate.slice(index, length);
  }

  @Override
  public DataBuffer retainedSlice(int index, int length) {
    return this.delegate.retainedSlice(index, length);
  }

  @Override
  public DataBuffer split(int index) {
    return this.delegate.split(index);
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return this.delegate.asByteBuffer();
  }

  @Override
  public ByteBuffer asByteBuffer(int index, int length) {
    return this.delegate.asByteBuffer(index, length);
  }

  @Override
  public ByteBuffer toByteBuffer() {
    return this.delegate.toByteBuffer();
  }

  @Override
  public ByteBuffer toByteBuffer(int index, int length) {
    return this.delegate.toByteBuffer(index, length);
  }

  @Override
  public void toByteBuffer(ByteBuffer dest) {
    this.delegate.toByteBuffer(dest);
  }

  @Override
  public void toByteBuffer(int srcPos, ByteBuffer dest, int destPos, int length) {
    this.delegate.toByteBuffer(srcPos, dest, destPos, length);
  }

  @Override
  public ByteBufferIterator readableByteBuffers() {
    return this.delegate.readableByteBuffers();
  }

  @Override
  public ByteBufferIterator writableByteBuffers() {
    return this.delegate.writableByteBuffers();
  }

  @Override
  public InputStream asInputStream() {
    return this.delegate.asInputStream();
  }

  @Override
  public InputStream asInputStream(boolean releaseOnClose) {
    return this.delegate.asInputStream(releaseOnClose);
  }

  @Override
  public OutputStream asOutputStream() {
    return this.delegate.asOutputStream();
  }

  @Override
  public String toString(Charset charset) {
    return this.delegate.toString(charset);
  }

  @Override
  public String toString(int index, int length, Charset charset) {
    return this.delegate.toString(index, length, charset);
  }

  @Override
  public boolean isAllocated() {
    return delegate.isAllocated();
  }

  @Override
  public boolean release() {
    return delegate.release();
  }

  @Override
  public DataBuffer retain() {
    return delegate.retain();
  }

  @Override
  public DataBuffer touch(Object hint) {
    return delegate.touch(hint);
  }
}
