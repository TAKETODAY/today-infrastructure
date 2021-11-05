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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.IntPredicate;

import cn.taketoday.lang.Assert;

/**
 * Provides a convenient implementation of the {@link DataBuffer} interface
 * that can be overridden to adapt the delegate.
 *
 * <p>These methods default to calling through to the wrapped delegate object.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class DataBufferWrapper implements DataBuffer {

  private final DataBuffer delegate;

  /**
   * Create a new {@code DataBufferWrapper} that wraps the given buffer.
   *
   * @param delegate the buffer to wrap
   */
  public DataBufferWrapper(DataBuffer delegate) {
    Assert.notNull(delegate, "Delegate must not be null");
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
  public int readableByteCount() {
    return this.delegate.readableByteCount();
  }

  @Override
  public int writableByteCount() {
    return this.delegate.writableByteCount();
  }

  @Override
  public int capacity() {
    return this.delegate.capacity();
  }

  @Override
  public DataBuffer capacity(int capacity) {
    return this.delegate.capacity(capacity);
  }

  @Override
  public DataBuffer ensureCapacity(int capacity) {
    return this.delegate.ensureCapacity(capacity);
  }

  @Override
  public int readPosition() {
    return this.delegate.readPosition();
  }

  @Override
  public DataBuffer readPosition(int readPosition) {
    return this.delegate.readPosition(readPosition);
  }

  @Override
  public int writePosition() {
    return this.delegate.writePosition();
  }

  @Override
  public DataBuffer writePosition(int writePosition) {
    return this.delegate.writePosition(writePosition);
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
  public DataBuffer read(byte[] destination) {
    return this.delegate.read(destination);
  }

  @Override
  public DataBuffer read(byte[] destination, int offset, int length) {
    return this.delegate.read(destination, offset, length);
  }

  @Override
  public DataBuffer write(byte b) {
    return this.delegate.write(b);
  }

  @Override
  public DataBuffer write(byte[] source) {
    return this.delegate.write(source);
  }

  @Override
  public DataBuffer write(byte[] source, int offset, int length) {
    return this.delegate.write(source, offset, length);
  }

  @Override
  public DataBuffer write(DataBuffer... buffers) {
    return this.delegate.write(buffers);
  }

  @Override
  public DataBuffer write(ByteBuffer... buffers) {
    return this.delegate.write(buffers);
  }

  @Override
  public DataBuffer write(CharSequence charSequence, Charset charset) {
    return this.delegate.write(charSequence, charset);
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
  public ByteBuffer asByteBuffer() {
    return this.delegate.asByteBuffer();
  }

  @Override
  public ByteBuffer asByteBuffer(int index, int length) {
    return this.delegate.asByteBuffer(index, length);
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

}
