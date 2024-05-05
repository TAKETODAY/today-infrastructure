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
package cn.taketoday.mock.api.fileupload;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * <p>
 * The data can be retrieved using <code>toByteArray()</code> and
 * <code>toString()</code>.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 * <p>
 * This is an alternative implementation of the {@link java.io.ByteArrayOutputStream}
 * class. The original implementation only allocates 32 bytes at the beginning.
 * As this class is designed for heavy duty it starts at 1024 bytes. In contrast
 * to the original it doesn't reallocate the whole memory block but allocates
 * additional buffers. This way no buffers need to be garbage collected and
 * the contents don't have to be copied to the new buffer. This class is
 * designed to behave exactly like the original. The only exception is the
 * deprecated toString(int) method that has been ignored.
 */
public class ByteArrayOutputStream extends OutputStream {

  static final int DEFAULT_SIZE = 1024;

  /** A singleton empty byte array. */
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  /** The list of buffers, which grows and never reduces. */
  private final List<byte[]> buffers = new ArrayList<>();
  /** The index of the current buffer. */
  private int currentBufferIndex;
  /** The total count of bytes in all the filled buffers. */
  private int filledBufferSum;
  /** The current buffer. */
  private byte[] currentBuffer;
  /** The total count of bytes written. */
  private int count;

  /**
   * Creates a new byte array output stream. The buffer capacity is
   * initially 1024 bytes, though its size increases if necessary.
   */
  public ByteArrayOutputStream() {
    this(DEFAULT_SIZE);
  }

  /**
   * Creates a new byte array output stream, with a buffer capacity of
   * the specified size, in bytes.
   *
   * @param size the initial size
   * @throws IllegalArgumentException if size is negative
   */
  public ByteArrayOutputStream(final int size) {
    if (size < 0) {
      throw new IllegalArgumentException(
              "Negative initial size: " + size);
    }
    synchronized(this) {
      needNewBuffer(size);
    }
  }

  /**
   * Makes a new buffer available either by allocating
   * a new one or re-cycling an existing one.
   *
   * @param newcount the size of the buffer if one is created
   */
  private void needNewBuffer(final int newcount) {
    if (currentBufferIndex < buffers.size() - 1) {
      //Recycling old buffer
      filledBufferSum += currentBuffer.length;

      currentBufferIndex++;
      currentBuffer = buffers.get(currentBufferIndex);
    }
    else {
      //Creating new buffer
      int newBufferSize;
      if (currentBuffer == null) {
        newBufferSize = newcount;
        filledBufferSum = 0;
      }
      else {
        newBufferSize = Math.max(
                currentBuffer.length << 1,
                newcount - filledBufferSum);
        filledBufferSum += currentBuffer.length;
      }

      currentBufferIndex++;
      currentBuffer = new byte[newBufferSize];
      buffers.add(currentBuffer);
    }
  }

  /**
   * Write the bytes to byte array.
   *
   * @param b the bytes to write
   * @param off The start offset
   * @param len The number of bytes to write
   */
  @Override
  public void write(final byte[] b, final int off, final int len) {
    if ((off < 0)
            || (off > b.length)
            || (len < 0)
            || ((off + len) > b.length)
            || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    }
    else if (len == 0) {
      return;
    }
    synchronized(this) {
      final int newcount = count + len;
      int remaining = len;
      int inBufferPos = count - filledBufferSum;
      while (remaining > 0) {
        final int part = Math.min(remaining, currentBuffer.length - inBufferPos);
        System.arraycopy(b, off + len - remaining, currentBuffer, inBufferPos, part);
        remaining -= part;
        if (remaining > 0) {
          needNewBuffer(newcount);
          inBufferPos = 0;
        }
      }
      count = newcount;
    }
  }

  /**
   * Write a byte to byte array.
   *
   * @param b the byte to write
   */
  @Override
  public synchronized void write(final int b) {
    int inBufferPos = count - filledBufferSum;
    if (inBufferPos == currentBuffer.length) {
      needNewBuffer(count + 1);
      inBufferPos = 0;
    }
    currentBuffer[inBufferPos] = (byte) b;
    count++;
  }

  /**
   * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
   * this class can be called after the stream has been closed without
   * generating an {@code IOException}.
   *
   * @throws IOException never (this method should not declare this exception
   * but it has to now due to backwards compatibility)
   */
  @Override
  public void close() throws IOException {
    //nop
  }

  /**
   * Writes the entire contents of this byte stream to the
   * specified output stream.
   *
   * @param out the output stream to write to
   * @throws IOException if an I/O error occurs, such as if the stream is closed
   * @see java.io.ByteArrayOutputStream#writeTo(OutputStream)
   */
  public synchronized void writeTo(final OutputStream out) throws IOException {
    int remaining = count;
    for (final byte[] buf : buffers) {
      final int c = Math.min(buf.length, remaining);
      out.write(buf, 0, c);
      remaining -= c;
      if (remaining == 0) {
        break;
      }
    }
  }

  /**
   * Gets the current contents of this byte stream as a byte array.
   * The result is independent of this stream.
   *
   * @return the current contents of this output stream, as a byte array
   * @see java.io.ByteArrayOutputStream#toByteArray()
   */
  public synchronized byte[] toByteArray() {
    int remaining = count;
    if (remaining == 0) {
      return EMPTY_BYTE_ARRAY;
    }
    final byte newbuf[] = new byte[remaining];
    int pos = 0;
    for (final byte[] buf : buffers) {
      final int c = Math.min(buf.length, remaining);
      System.arraycopy(buf, 0, newbuf, pos, c);
      pos += c;
      remaining -= c;
      if (remaining == 0) {
        break;
      }
    }
    return newbuf;
  }
}
