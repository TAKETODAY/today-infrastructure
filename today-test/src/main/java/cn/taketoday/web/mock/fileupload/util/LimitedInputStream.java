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
package cn.taketoday.web.mock.fileupload.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream, which limits its data size. This stream is
 * used, if the content length is unknown.
 */
public abstract class LimitedInputStream extends FilterInputStream implements Closeable {

  /**
   * The maximum size of an item, in bytes.
   */
  private final long sizeMax;

  /**
   * The current number of bytes.
   */
  private long count;

  /**
   * Whether this stream is already closed.
   */
  private boolean closed;

  /**
   * Creates a new instance.
   *
   * @param inputStream The input stream, which shall be limited.
   * @param pSizeMax The limit; no more than this number of bytes
   * shall be returned by the source stream.
   */
  public LimitedInputStream(final InputStream inputStream, final long pSizeMax) {
    super(inputStream);
    sizeMax = pSizeMax;
  }

  /**
   * Called to indicate, that the input streams limit has
   * been exceeded.
   *
   * @param pSizeMax The input streams limit, in bytes.
   * @param pCount The actual number of bytes.
   * @throws IOException The called method is expected
   * to raise an IOException.
   */
  protected abstract void raiseError(long pSizeMax, long pCount)
          throws IOException;

  /**
   * Called to check, whether the input streams
   * limit is reached.
   *
   * @throws IOException The given limit is exceeded.
   */
  private void checkLimit() throws IOException {
    if (count > sizeMax) {
      raiseError(sizeMax, count);
    }
  }

  /**
   * Reads the next byte of data from this input stream. The value
   * byte is returned as an {@code int} in the range
   * {@code 0} to {@code 255}. If no byte is available
   * because the end of the stream has been reached, the value
   * {@code -1} is returned. This method blocks until input data
   * is available, the end of the stream is detected, or an exception
   * is thrown.
   * <p>
   * This method
   * simply performs {@code in.read()} and returns the result.
   *
   * @return the next byte of data, or {@code -1} if the end of the
   * stream is reached.
   * @throws IOException if an I/O error occurs.
   * @see FilterInputStream#in
   */
  @Override
  public int read() throws IOException {
    final int res = super.read();
    if (res != -1) {
      count++;
      checkLimit();
    }
    return res;
  }

  /**
   * Reads up to {@code len} bytes of data from this input stream
   * into an array of bytes. If {@code len} is not zero, the method
   * blocks until some input is available; otherwise, no
   * bytes are read and {@code 0} is returned.
   * <p>
   * This method simply performs {@code in.read(b, off, len)}
   * and returns the result.
   *
   * @param b the buffer into which the data is read.
   * @param off The start offset in the destination array
   * {@code b}.
   * @param len the maximum number of bytes read.
   * @return the total number of bytes read into the buffer, or
   * {@code -1} if there is no more data because the end of
   * the stream has been reached.
   * @throws NullPointerException If {@code b} is {@code null}.
   * @throws IndexOutOfBoundsException If {@code off} is negative,
   * {@code len} is negative, or {@code len} is greater than
   * {@code b.length - off}
   * @throws IOException if an I/O error occurs.
   * @see FilterInputStream#in
   */
  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    final int res = super.read(b, off, len);
    if (res > 0) {
      count += res;
      checkLimit();
    }
    return res;
  }

  /**
   * Returns, whether this stream is already closed.
   *
   * @return True, if the stream is closed, otherwise false.
   * @throws IOException An I/O error occurred.
   */
  @Override
  public boolean isClosed() throws IOException {
    return closed;
  }

  /**
   * Closes this input stream and releases any system resources
   * associated with the stream.
   * This
   * method simply performs {@code in.close()}.
   *
   * @throws IOException if an I/O error occurs.
   * @see FilterInputStream#in
   */
  @Override
  public void close() throws IOException {
    closed = true;
    super.close();
  }

}
