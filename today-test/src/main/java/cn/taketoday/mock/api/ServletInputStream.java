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

package cn.taketoday.mock.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides an input stream for reading binary data from a client request, including an efficient <code>readLine</code>
 * method for reading data one line at a time. With some protocols, such as HTTP POST and PUT, a
 * <code>ServletInputStream</code> object can be used to read data sent from the client.
 *
 * <p>
 * A <code>ServletInputStream</code> object is normally retrieved via the {@link ServletRequest#getInputStream} method.
 *
 *
 * <p>
 * This is an abstract class that a servlet container implements. Subclasses of this class must implement the
 * <code>java.io.InputStream.read()</code> method.
 *
 * @author Various
 * @see ServletRequest
 */
public abstract class ServletInputStream extends InputStream {

  /**
   * Does nothing, because this is an abstract class.
   */
  protected ServletInputStream() {
  }

  /**
   * Reads the input stream, one line at a time. Starting at an offset, reads bytes into an array, until it reads a
   * certain number of bytes or reaches a newline character, which it reads into the array as well.
   *
   * <p>
   * This method returns -1 if it reaches the end of the input stream before reading the maximum number of bytes.
   *
   * @param b an array of bytes into which data is read
   * @param off an integer specifying the character at which this method begins reading
   * @param len an integer specifying the maximum number of bytes to read
   * @return an integer specifying the actual number of bytes read, or -1 if the end of the stream is reached
   * @throws IOException if an input or output exception has occurred
   */
  public int readLine(byte[] b, int off, int len) throws IOException {

    if (len <= 0) {
      return 0;
    }
    int count = 0, c;

    while ((c = read()) != -1) {
      b[off++] = (byte) c;
      count++;
      if (c == '\n' || count == len) {
        break;
      }
    }
    return count > 0 ? count : -1;
  }

  /**
   * Returns true when all the data from the stream has been read else it returns false.
   *
   * @return <code>true</code> when all data for this particular request has been read, otherwise returns
   * <code>false</code>.
   * @since Servlet 3.1
   */
  public abstract boolean isFinished();

  /**
   * Returns true if data can be read without blocking else returns false.
   * <p>
   * If this method returns false and a {@link ReadListener} has been set via {@link #setReadListener(ReadListener)}, then
   * the container will subsequently invoke {@link ReadListener#onDataAvailable()} (or
   * {@link ReadListener#onAllDataRead()}) once data (or EOF) has become available. Other than the initial call,
   * {@link ReadListener#onDataAvailable()} will only be called if and only if this method is called and returns false.
   *
   * @return <code>true</code> if data can be obtained without blocking, otherwise returns <code>false</code>.
   * @see ReadListener
   * @since Servlet 3.1
   */
  public abstract boolean isReady();

  /**
   * Instructs the <code>ServletInputStream</code> to invoke the provided {@link ReadListener} when it is possible to read
   *
   * @param readListener the {@link ReadListener} that should be notified when it's possible to read.
   * @throws IllegalStateException if one of the following conditions is true
   * <ul>
   * <li>the associated request is neither upgraded nor the async started
   * <li>setReadListener is called more than once within the scope of the same request.
   * </ul>
   * @throws NullPointerException if readListener is null
   * @since Servlet 3.1
   */
  public abstract void setReadListener(ReadListener readListener);
}
