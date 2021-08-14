/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server.light;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author TODAY 2021/4/14 21:01
 * form {@link ByteArrayOutputStream}
 */
public class ResponseOutputBuffer extends OutputStream {

  /**
   * The buffer where data is stored.
   */
  protected byte[] buf;

  /**
   * The number of valid bytes in the buffer.
   */
  protected int count;

  /**
   * Creates a new byte array output stream. The buffer capacity is
   * initially 32 bytes, though its size increases if necessary.
   */
  public ResponseOutputBuffer() {
    this(32);
  }

  /**
   * Creates a new byte array output stream, with a buffer capacity of
   * the specified size, in bytes.
   *
   * @param size
   *         the initial size.
   *
   * @throws IllegalArgumentException
   *         if size is negative.
   */
  public ResponseOutputBuffer(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Negative initial size: " + size);
    }
    buf = new byte[size];
  }

  protected ResponseOutputBuffer(byte[] buf) {
    this.buf = buf; // none null
    this.count = buf.length;
  }

  public void setBuffer(byte[] buffer) {
    this.buf = buffer;
  }

  /**
   * Increases the capacity if necessary to ensure that it can hold
   * at least the number of elements specified by the minimum
   * capacity argument.
   *
   * @param minCapacity
   *         the desired minimum capacity
   *
   * @throws OutOfMemoryError
   *         if {@code minCapacity < 0}.  This is
   *         interpreted as a request for the unsatisfiably large capacity
   *         {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
   */
  private void ensureCapacity(int minCapacity) {
    // overflow-conscious code
    if (minCapacity - buf.length > 0)
      grow(minCapacity);
  }

  /**
   * The maximum size of array to allocate.
   * Some VMs reserve some header words in an array.
   * Attempts to allocate larger arrays may result in
   * OutOfMemoryError: Requested array size exceeds VM limit
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  /**
   * Increases the capacity to ensure that it can hold at least the
   * number of elements specified by the minimum capacity argument.
   *
   * @param minCapacity
   *         the desired minimum capacity
   */
  private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = buf.length;
    int newCapacity = oldCapacity << 1;
    if (newCapacity - minCapacity < 0)
      newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
      newCapacity = hugeCapacity(minCapacity);
    buf = Arrays.copyOf(buf, newCapacity);
  }

  private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
      throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
  }

  /**
   * Writes the specified byte to this byte array output stream.
   *
   * @param b
   *         the byte to be written.
   */
  @Override
  public void write(int b) {
    ensureCapacity(count + 1);
    buf[count] = (byte) b;
    count += 1;
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array
   * starting at offset <code>off</code> to this byte array output stream.
   *
   * @param b
   *         the data.
   * @param off
   *         the start offset in the data.
   * @param len
   *         the number of bytes to write.
   */
  @Override
  public void write(byte[] b, int off, int len) {
    if ((off < 0) || (off > b.length) || (len < 0) ||
            ((off + len) - b.length > 0)) {
      throw new IndexOutOfBoundsException();
    }
    ensureCapacity(count + len);
    System.arraycopy(b, off, buf, count, len);
    count += len;
  }

  /**
   * Writes the complete contents of this byte array output stream to
   * the specified output stream argument, as if by calling the output
   * stream's write method using <code>out.write(buf, 0, count)</code>.
   *
   * @param out
   *         the output stream to which to write the data.
   *
   * @throws IOException
   *         if an I/O error occurs.
   */
  public void writeTo(OutputStream out) throws IOException {
    out.write(buf, 0, count);
  }

  /**
   * Resets the <code>count</code> field of this byte array output
   * stream to zero, so that all currently accumulated output in the
   * output stream is discarded. The output stream can be used again,
   * reusing the already allocated buffer space.
   *
   * @see java.io.ByteArrayInputStream#count
   */
  public void reset() {
    count = 0;
  }

  /**
   * Creates a newly allocated byte array. Its size is the current
   * size of this output stream and the valid contents of the buffer
   * have been copied into it.
   *
   * @return the current contents of this output stream, as a byte array.
   *
   * @see java.io.ByteArrayOutputStream#size()
   */
  public byte[] toByteArray() {
    return Arrays.copyOf(buf, count);
  }

  /**
   * Returns the current size of the buffer.
   *
   * @return the value of the <code>count</code> field, which is the number
   * of valid bytes in this output stream.
   *
   * @see java.io.ByteArrayOutputStream#count
   */
  public int size() {
    return count;
  }

  /**
   * Converts the buffer's contents into a string decoding bytes using the
   * platform's default character set. The length of the new <tt>String</tt>
   * is a function of the character set, and hence may not be equal to the
   * size of the buffer.
   *
   * <p> This method always replaces malformed-input and unmappable-character
   * sequences with the default replacement string for the platform's
   * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
   * class should be used when more control over the decoding process is
   * required.
   *
   * @return String decoded from the buffer's contents.
   */
  @Override
  public String toString() {
    return new String(buf, 0, count);
  }

  /**
   * Converts the buffer's contents into a string by decoding the bytes using
   * the named {@link java.nio.charset.Charset charset}. The length of the new
   * <tt>String</tt> is a function of the charset, and hence may not be equal
   * to the length of the byte array.
   *
   * <p> This method always replaces malformed-input and unmappable-character
   * sequences with this charset's default replacement string. The {@link
   * java.nio.charset.CharsetDecoder} class should be used when more control
   * over the decoding process is required.
   *
   * @param charset
   *         {@link java.nio.charset.Charset charset}
   *
   * @return String decoded from the buffer's contents.
   */
  public String toString(Charset charset) {
    return new String(buf, 0, count, charset);
  }

  /**
   * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
   * this class can be called after the stream has been closed without
   * generating an <tt>IOException</tt>.
   */
  @Override
  public void close() throws IOException {
    //no-op
  }

  // static

  public static ResponseOutputBuffer ofBytes(byte[] buffer) {
    return new ResponseOutputBuffer(buffer);
  }

}
