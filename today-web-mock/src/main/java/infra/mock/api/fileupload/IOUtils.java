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
package infra.mock.api.fileupload;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General IO stream manipulation utilities.
 * <p>
 * This class provides static utility methods for input/output operations.
 * <ul>
 * <li>closeQuietly - these methods close a stream ignoring nulls and exceptions
 * <li>toXxx/read - these methods read data from a stream
 * <li>write - these methods write data to a stream
 * <li>copy - these methods copy all the data from one stream to another
 * <li>contentEquals - these methods compare the content of two streams
 * </ul>
 * <p>
 * The byte-to-char methods and char-to-byte methods involve a conversion step.
 * Two methods are provided in each case, one that uses the platform default
 * encoding and the other which allows you to specify an encoding. You are
 * encouraged to always specify an encoding because relying on the platform
 * default can lead to unexpected results, for example when moving from
 * development to production.
 * <p>
 * All the methods in this class that read a stream are buffered internally.
 * This means that there is no cause to use a <code>BufferedInputStream</code>
 * or <code>BufferedReader</code>. The default buffer size of 4K has been shown
 * to be efficient in tests.
 * <p>
 * Wherever possible, the methods in this class do <em>not</em> flush or close
 * the stream. This is to avoid making non-portable assumptions about the
 * streams' origin and further use. Thus the caller is still responsible for
 * closing streams after use.
 * <p>
 * Origin of code: Excalibur.
 */
public class IOUtils {
  // NOTE: This class is focused on InputStream, OutputStream, Reader and
  // Writer. Each method should take at least one of these as a parameter,
  // or return one of them.

  /**
   * Represents the end-of-file (or stream).
   *
   * @since IO 2.5 (made public)
   */
  public static final int EOF = -1;

  /**
   * The default buffer size ({@value}) to use for
   * {@link #copyLarge(InputStream, OutputStream)}.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Closes a <code>Closeable</code> unconditionally.
   * <p>
   * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored. This is typically used in
   * finally blocks.
   * <p>
   * Example code:
   * </p>
   * <pre>
   * Closeable closeable = null;
   * try {
   *     closeable = new FileReader(&quot;foo.txt&quot;);
   *     // process closeable
   *     closeable.close();
   * } catch (Exception e) {
   *     // error handling
   * } finally {
   *     IOUtils.closeQuietly(closeable);
   * }
   * </pre>
   * <p>
   * Closing all streams:
   * </p>
   * <pre>
   * try {
   *     return IOUtils.copy(inputStream, outputStream);
   * } finally {
   *     IOUtils.closeQuietly(inputStream);
   *     IOUtils.closeQuietly(outputStream);
   * }
   * </pre>
   *
   * @param closeable the objects to close, may be null or already closed
   * @since IO 2.0
   */
  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    }
    catch (final IOException ioe) {
      // ignore
    }
  }

  // copy from InputStream
  //-----------------------------------------------------------------------

  /**
   * Copies bytes from an <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of bytes cannot be returned as an int. For large streams
   * use the <code>copyLarge(InputStream, OutputStream)</code> method.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since IO 1.1
   */
  public static int copy(final InputStream input, final OutputStream output) throws IOException {
    final long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copies bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since IO 1.3
   */
  public static long copyLarge(final InputStream input, final OutputStream output)
          throws IOException {

    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Reads bytes from an input stream.
   * This implementation guarantees that it will read as many bytes
   * as possible before giving up; this may not always be the case for
   * subclasses of {@link InputStream}.
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset initial offset into buffer
   * @param length length to read, must be &gt;= 0
   * @return actual length read; may be less than requested if EOF was reached
   * @throws IOException if a read error occurs
   * @since IO 2.2
   */
  public static int read(final InputStream input, final byte[] buffer, final int offset, final int length)
          throws IOException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must not be negative: " + length);
    }
    int remaining = length;
    while (remaining > 0) {
      final int location = length - remaining;
      final int count = input.read(buffer, offset + location, remaining);
      if (EOF == count) { // EOF
        break;
      }
      remaining -= count;
    }
    return length - remaining;
  }

  /**
   * Reads the requested number of bytes or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link InputStream#read(byte[], int, int)} may
   * not read as many bytes as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset initial offset into buffer
   * @param length length to read, must be &gt;= 0
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of bytes read was incorrect
   * @since IO 2.2
   */
  public static void readFully(final InputStream input, final byte[] buffer, final int offset, final int length)
          throws IOException {
    final int actual = read(input, buffer, offset, length);
    if (actual != length) {
      throw new EOFException("Length to read: " + length + " actual: " + actual);
    }
  }

  /**
   * Reads the requested number of bytes or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link InputStream#read(byte[], int, int)} may
   * not read as many bytes as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of bytes read was incorrect
   * @since IO 2.2
   */
  public static void readFully(final InputStream input, final byte[] buffer) throws IOException {
    readFully(input, buffer, 0, buffer.length);
  }
}
