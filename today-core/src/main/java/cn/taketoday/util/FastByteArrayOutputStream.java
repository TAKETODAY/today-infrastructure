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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Iterator;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * A speedy alternative to {@link java.io.ByteArrayOutputStream}. Note that
 * this variant does <i>not</i> extend {@code ByteArrayOutputStream}, unlike
 * its sibling {@link ResizableByteArrayOutputStream}.
 *
 * <p>Unlike {@link java.io.ByteArrayOutputStream}, this implementation is backed
 * by an {@link java.util.ArrayDeque} of {@code byte[]} instead of 1 constantly
 * resizing {@code byte[]}. It does not copy buffers when it gets expanded.
 *
 * <p>The initial buffer is only created when the stream is first written.
 * There is also no copying of the internal buffer if its contents is extracted
 * with the {@link #writeTo(OutputStream)} method.
 *
 * @author Craig Andrews
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #resize
 * @see ResizableByteArrayOutputStream
 * @since 4.0 2021/8/21 01:18
 */
public class FastByteArrayOutputStream extends OutputStream {

  private static final int DEFAULT_BLOCK_SIZE = 256;

  // The buffers used to store the content bytes
  private final ArrayDeque<byte[]> buffers = new ArrayDeque<>();

  // The size, in bytes, to use when allocating the first byte[]
  private final int initialBlockSize;

  // The size, in bytes, to use when allocating the next byte[]
  private int nextBlockSize = 0;

  // The number of bytes in previous buffers.
  // (The number of bytes in the current buffer is in 'index'.)
  private int alreadyBufferedSize = 0;

  // The index in the byte[] found at buffers.getLast() to be written next
  private int index = 0;

  // Is the stream closed?
  private boolean closed = false;

  /**
   * Create a new <code>FastByteArrayOutputStream</code>
   * with the default initial capacity of 256 bytes.
   */
  public FastByteArrayOutputStream() {
    this(DEFAULT_BLOCK_SIZE);
  }

  /**
   * Create a new <code>FastByteArrayOutputStream</code>
   * with the specified initial capacity.
   *
   * @param initialBlockSize the initial buffer size in bytes
   */
  public FastByteArrayOutputStream(int initialBlockSize) {
    Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
    this.initialBlockSize = initialBlockSize;
    this.nextBlockSize = initialBlockSize;
  }

  // Overridden methods

  @Override
  public void write(final int datum) throws IOException {
    if (this.closed) {
      throw new IOException("Stream closed");
    }
    else {
      final ArrayDeque<byte[]> buffers = this.buffers;
      if (buffers.peekLast() == null || buffers.getLast().length == this.index) {
        addBuffer(1);
      }
      // store the byte
      buffers.getLast()[this.index++] = (byte) datum;
    }
  }

  @Override
  public void write(@NonNull byte[] data, int offset, int length) throws IOException {
    if (offset < 0 || offset + length > data.length || length < 0) {
      throw new IndexOutOfBoundsException();
    }
    else if (this.closed) {
      throw new IOException("Stream closed");
    }
    else {
      final ArrayDeque<byte[]> buffers = this.buffers;
      if (buffers.peekLast() == null || buffers.getLast().length == this.index) {
        addBuffer(length);
      }
      if (this.index + length > buffers.getLast().length) {
        int pos = offset;
        do {
          if (this.index == buffers.getLast().length) {
            addBuffer(length);
          }
          int copyLength = buffers.getLast().length - this.index;
          if (length < copyLength) {
            copyLength = length;
          }
          System.arraycopy(data, pos, buffers.getLast(), this.index, copyLength);
          pos += copyLength;
          this.index += copyLength;
          length -= copyLength;
        }
        while (length > 0);
      }
      else {
        // copy in the sub-array
        System.arraycopy(data, offset, buffers.getLast(), this.index, length);
        this.index += length;
      }
    }
  }

  @Override
  public void close() {
    this.closed = true;
  }

  /**
   * Convert the buffer's contents into a string decoding bytes using the
   * platform's default character set. The length of the new <tt>String</tt>
   * is a function of the character set, and hence may not be equal to the
   * size of the buffer.
   * <p>This method always replaces malformed-input and unmappable-character
   * sequences with the default replacement string for the platform's
   * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
   * class should be used when more control over the decoding process is
   * required.
   *
   * @return a String decoded from the buffer's contents
   */
  @Override
  public String toString() {
    return toString(Charset.defaultCharset());
  }

  /**
   * Convert this stream's contents to a string by decoding the bytes using the
   * specified {@link Charset}.
   *
   * @param charset the {@link Charset} to use to decode the bytes
   * @return a String decoded from this stream's contents
   * @see #toString()
   */
  public String toString(Charset charset) {
    if (size() == 0) {
      return "";
    }
    if (this.buffers.size() == 1) {
      return new String(this.buffers.getFirst(), 0, this.index, charset);
    }
    return new String(toByteArrayUnsafe(), charset);
  }

  // Custom methods

  /**
   * Return the number of bytes stored in this <code>FastByteArrayOutputStream</code>.
   */
  public int size() {
    return (this.alreadyBufferedSize + this.index);
  }

  /**
   * Convert the stream's data to a byte array and return the byte array.
   * <p>Also replaces the internal structures with the byte array to conserve memory:
   * if the byte array is being made anyways, mind as well as use it. This approach
   * also means that if this method is called twice without any writes in between,
   * the second call is a no-op.
   * <p>This method is "unsafe" as it returns the internal buffer.
   * Callers should not modify the returned buffer.
   *
   * @return the current contents of this output stream, as a byte array.
   * @see #size()
   * @see #toByteArray()
   */
  public byte[] toByteArrayUnsafe() {
    final int totalSize = size();
    if (totalSize == 0) {
      return Constant.EMPTY_BYTES;
    }
    resize(totalSize);
    return this.buffers.getFirst();
  }

  /**
   * Creates a newly allocated byte array.
   * <p>Its size is the current
   * size of this output stream and the valid contents of the buffer
   * have been copied into it.</p>
   *
   * @return the current contents of this output stream, as a byte array.
   * @see #size()
   * @see #toByteArrayUnsafe()
   */
  public byte[] toByteArray() {
    return toByteArrayUnsafe().clone();
  }

  /**
   * Reset the contents of this <code>FastByteArrayOutputStream</code>.
   * <p>All currently accumulated output in the output stream is discarded.
   * The output stream can be used again.
   */
  public void reset() {
    this.buffers.clear();
    this.nextBlockSize = this.initialBlockSize;
    this.closed = false;
    this.index = 0;
    this.alreadyBufferedSize = 0;
  }

  /**
   * Get an {@link InputStream} to retrieve the data in this OutputStream.
   * <p>Note that if any methods are called on the OutputStream
   * (including, but not limited to, any of the write methods, {@link #reset()},
   * {@link #toByteArray()}, and {@link #toByteArrayUnsafe()}) then the
   * {@link java.io.InputStream}'s behavior is undefined.
   *
   * @return {@link InputStream} of the contents of this OutputStream
   */
  public InputStream getInputStream() {
    return new FastByteArrayInputStream(this);
  }

  /**
   * Write the buffers content to the given OutputStream.
   *
   * @param out the OutputStream to write to
   */
  public void writeTo(OutputStream out) throws IOException {
    Iterator<byte[]> it = this.buffers.iterator();
    while (it.hasNext()) {
      byte[] bytes = it.next();
      if (it.hasNext()) {
        out.write(bytes, 0, bytes.length);
      }
      else {
        out.write(bytes, 0, this.index);
      }
    }
  }

  /**
   * Resize the internal buffer size to a specified capacity.
   *
   * @param targetCapacity the desired size of the buffer
   * @throws IllegalArgumentException if the given capacity is smaller than
   * the actual size of the content stored in the buffer already
   * @see FastByteArrayOutputStream#size()
   */
  public void resize(final int targetCapacity) {
    final int totalSize = size();
    Assert.isTrue(targetCapacity >= totalSize, "New capacity must not be smaller than current size");
    final ArrayDeque<byte[]> buffers = this.buffers;
    if (buffers.peekFirst() == null) {
      this.nextBlockSize = targetCapacity - totalSize;
    }
    else if (totalSize != targetCapacity || buffers.getFirst().length != targetCapacity) {
      // not already at the targetCapacity
      int pos = 0;
      final int index = this.index;
      final byte[] data = new byte[targetCapacity];
      final Iterator<byte[]> it = buffers.iterator();
      while (it.hasNext()) {
        final byte[] bytes = it.next();
        if (it.hasNext()) {
          System.arraycopy(bytes, 0, data, pos, bytes.length);
          pos += bytes.length;
        }
        else {
          System.arraycopy(bytes, 0, data, pos, index);
        }
      }
      buffers.clear();
      buffers.add(data);
      this.index = totalSize;
      this.alreadyBufferedSize = 0;
    }
  }

  /**
   * Create a new buffer and store it in the ArrayDeque.
   * <p>Adds a new buffer that can store at least {@code minCapacity} bytes.
   */
  private void addBuffer(int minCapacity) {
    if (this.buffers.peekLast() != null) {
      this.alreadyBufferedSize += this.index;
      this.index = 0;
    }
    if (this.nextBlockSize < minCapacity) {
      this.nextBlockSize = nextPowerOf2(minCapacity);
    }
    this.buffers.add(new byte[this.nextBlockSize]);
    this.nextBlockSize *= 2;  // block size doubles each time
  }

  /**
   * Get the next power of 2 of a number (ex, the next power of 2 of 119 is 128).
   */
  private static int nextPowerOf2(int val) {
    val--;
    val = (val >> 1) | val;
    val = (val >> 2) | val;
    val = (val >> 4) | val;
    val = (val >> 8) | val;
    val = (val >> 16) | val;
    val++;
    return val;
  }

  /**
   * An implementation of {@link java.io.InputStream} that reads from a given
   * <code>FastByteArrayOutputStream</code>.
   */
  private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {

    private final FastByteArrayOutputStream fastByteArrayOutputStream;

    private final Iterator<byte[]> buffersIterator;

    @Nullable
    private byte[] currentBuffer;

    private int currentBufferLength = 0;

    private int nextIndexInCurrentBuffer = 0;

    private int totalBytesRead = 0;

    /**
     * Create a new <code>FastByteArrayOutputStreamInputStream</code> backed
     * by the given <code>FastByteArrayOutputStream</code>.
     */
    public FastByteArrayInputStream(FastByteArrayOutputStream outputStream) {
      final ArrayDeque<byte[]> buffers = outputStream.buffers;
      final Iterator<byte[]> buffersIterator = buffers.iterator();
      if (buffersIterator.hasNext()) {
        final byte[] currentBuffer = buffersIterator.next();
        if (currentBuffer == buffers.getLast()) {
          this.currentBufferLength = outputStream.index;
        }
        else {
          this.currentBufferLength = (currentBuffer != null ? currentBuffer.length : 0);
        }
        this.currentBuffer = currentBuffer;
      }
      this.buffersIterator = buffersIterator;
      this.fastByteArrayOutputStream = outputStream;
    }

    @Override
    public int read() {
      if (this.currentBuffer == null) {
        // This stream doesn't have any data in it...
        return -1;
      }
      else {
        if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
          this.totalBytesRead++;
          return this.currentBuffer[this.nextIndexInCurrentBuffer++] & 0xFF;
        }
        else {
          if (this.buffersIterator.hasNext()) {
            this.currentBuffer = this.buffersIterator.next();
            updateCurrentBufferLength();
            this.nextIndexInCurrentBuffer = 0;
          }
          else {
            this.currentBuffer = null;
          }
          return read();
        }
      }
    }

    @Override
    public int read(@NonNull byte[] b) {
      return read(b, 0, b.length);
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) {
      if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      }
      else if (len == 0) {
        return 0;
      }
      else {
        if (this.currentBuffer == null) {
          // This stream doesn't have any data in it...
          return -1;
        }
        else {
          final int nextIndexInCurrentBuffer = this.nextIndexInCurrentBuffer;
          if (nextIndexInCurrentBuffer < this.currentBufferLength) {
            int bytesToCopy = Math.min(len, this.currentBufferLength - nextIndexInCurrentBuffer);
            System.arraycopy(this.currentBuffer, nextIndexInCurrentBuffer, b, off, bytesToCopy);
            this.totalBytesRead += bytesToCopy;
            this.nextIndexInCurrentBuffer += bytesToCopy;
            int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
            return bytesToCopy + Math.max(remaining, 0);
          }
          else {
            if (this.buffersIterator.hasNext()) {
              this.currentBuffer = this.buffersIterator.next();
              updateCurrentBufferLength();
              this.nextIndexInCurrentBuffer = 0;
            }
            else {
              this.currentBuffer = null;
            }
            return read(b, off, len);
          }
        }
      }
    }

    @Override
    public long skip(long n) throws IOException {
      if (n > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
      }
      else if (n == 0) {
        return 0;
      }
      else if (n < 0) {
        throw new IllegalArgumentException("n must be 0 or greater: " + n);
      }
      int len = (int) n;
      if (this.currentBuffer == null) {
        // This stream doesn't have any data in it...
        return 0;
      }
      else {
        if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
          int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
          this.totalBytesRead += bytesToSkip;
          this.nextIndexInCurrentBuffer += bytesToSkip;
          return (bytesToSkip + skip(len - bytesToSkip));
        }
        else {
          if (this.buffersIterator.hasNext()) {
            this.currentBuffer = this.buffersIterator.next();
            updateCurrentBufferLength();
            this.nextIndexInCurrentBuffer = 0;
          }
          else {
            this.currentBuffer = null;
          }
          return skip(len);
        }
      }
    }

    @Override
    public int available() {
      return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
    }

    /**
     * Update the message digest with the remaining bytes in this stream.
     *
     * @param messageDigest the message digest to update
     */
    @Override
    public void updateMessageDigest(MessageDigest messageDigest) {
      updateMessageDigest(messageDigest, available());
    }

    /**
     * Update the message digest with the next len bytes in this stream.
     * Avoids creating new byte arrays and use internal buffers for performance.
     *
     * @param messageDigest the message digest to update
     * @param len how many bytes to read from this stream and use to update the message digest
     */
    @Override
    public void updateMessageDigest(MessageDigest messageDigest, int len) {
      if (this.currentBuffer != null) {
        if (len < 0) {
          throw new IllegalArgumentException("len must be 0 or greater: " + len);
        }
        else if (len > 0) {
          final int nextIndexInCurrentBuffer = this.nextIndexInCurrentBuffer;
          if (nextIndexInCurrentBuffer < this.currentBufferLength) {
            int bytesToCopy = Math.min(len, this.currentBufferLength - nextIndexInCurrentBuffer);
            messageDigest.update(this.currentBuffer, nextIndexInCurrentBuffer, bytesToCopy);
            this.nextIndexInCurrentBuffer += bytesToCopy;
            updateMessageDigest(messageDigest, len - bytesToCopy);
          }
          else {
            if (this.buffersIterator.hasNext()) {
              this.currentBuffer = this.buffersIterator.next();
              updateCurrentBufferLength();
              this.nextIndexInCurrentBuffer = 0;
            }
            else {
              this.currentBuffer = null;
            }
            updateMessageDigest(messageDigest, len);
          }
        }
      }
      // This stream doesn't have any data in it...
    }

    private void updateCurrentBufferLength() {
      if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
        this.currentBufferLength = this.fastByteArrayOutputStream.index;
      }
      else {
        this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
      }
    }
  }

  abstract static class UpdateMessageDigestInputStream extends InputStream {

    /**
     * Update the message digest with the rest of the bytes in this stream.
     * <p>Using this method is more optimized since it avoids creating new
     * byte arrays for each call.
     *
     * @param messageDigest the message digest to update
     * @throws IOException when propagated from {@link #read()}
     */
    public void updateMessageDigest(MessageDigest messageDigest) throws IOException {
      int data;
      while ((data = read()) != -1) {
        messageDigest.update((byte) data);
      }
    }

    /**
     * Update the message digest with the next len bytes in this stream.
     * <p>Using this method is more optimized since it avoids creating new
     * byte arrays for each call.
     *
     * @param messageDigest the message digest to update
     * @param len how many bytes to read from this stream and use to update the message digest
     * @throws IOException when propagated from {@link #read()}
     */
    public void updateMessageDigest(MessageDigest messageDigest, int len) throws IOException {
      int data;
      int bytesRead = 0;
      while (bytesRead < len && (data = read()) != -1) {
        messageDigest.update((byte) data);
        bytesRead++;
      }
    }

  }
}
