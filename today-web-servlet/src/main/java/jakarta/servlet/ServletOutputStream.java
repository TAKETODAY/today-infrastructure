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

package jakarta.servlet;

import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides an output stream for sending binary data to the client. A <code>ServletOutputStream</code> object is
 * normally retrieved via the {@link ServletResponse#getOutputStream} method.
 *
 * <p>
 * This is an abstract class that the servlet container implements. Subclasses of this class must implement the
 * <code>java.io.OutputStream.write(int)</code> method.
 *
 * @author Various
 * @see ServletResponse
 */
public abstract class ServletOutputStream extends OutputStream {

  /**
   * Does nothing, because this is an abstract class.
   */
  protected ServletOutputStream() {
  }

  /**
   * Writes a <code>String</code> to the client, without a carriage return-line feed (CRLF) character at the end.
   *
   * @param s the <code>String</code> to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(String s) throws IOException {
    if (s == null)
      s = "null";
    int len = s.length();
    byte[] out = new byte[len];
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);

      //
      // XXX NOTE: This is clearly incorrect for many strings,
      // but is the only consistent approach within the current
      // servlet framework. It must suffice until servlet output
      // streams properly encode their output.
      //
      if ((c & 0xff00) != 0) { // high order byte must be zero
        throw new CharConversionException("Not an ISO 8859-1 character: %s".formatted(c));
      }
      out[i] = (byte) (0xff & c);
    }
    write(out, 0, len);
  }

  /**
   * Writes a <code>boolean</code> value to the client, with no carriage return-line feed (CRLF) character at the end.
   *
   * @param b the <code>boolean</code> value to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(boolean b) throws IOException {
    print(Boolean.toString(b));
  }

  /**
   * Writes a character to the client, with no carriage return-line feed (CRLF) at the end.
   *
   * @param c the character to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(char c) throws IOException {
    print(String.valueOf(c));
  }

  /**
   * Writes an int to the client, with no carriage return-line feed (CRLF) at the end.
   *
   * @param i the int to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(int i) throws IOException {
    print(String.valueOf(i));
  }

  /**
   * Writes a <code>long</code> value to the client, with no carriage return-line feed (CRLF) at the end.
   *
   * @param l the <code>long</code> value to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(long l) throws IOException {
    print(String.valueOf(l));
  }

  /**
   * Writes a <code>float</code> value to the client, with no carriage return-line feed (CRLF) at the end.
   *
   * @param f the <code>float</code> value to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(float f) throws IOException {
    print(String.valueOf(f));
  }

  /**
   * Writes a <code>double</code> value to the client, with no carriage return-line feed (CRLF) at the end.
   *
   * @param d the <code>double</code> value to send to the client
   * @throws IOException if an input or output exception occurred
   */
  public void print(double d) throws IOException {
    print(String.valueOf(d));
  }

  /**
   * Writes a carriage return-line feed (CRLF) to the client.
   *
   * @throws IOException if an input or output exception occurred
   */
  public void println() throws IOException {
    print("\r\n");
  }

  /**
   * Writes a <code>String</code> to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param s the <code>String</code> to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(String s) throws IOException {
    print(s == null ? "null\r\n" : (s + "\r\n"));
  }

  /**
   * Writes a <code>boolean</code> value to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param b the <code>boolean</code> value to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(boolean b) throws IOException {
    println(Boolean.toString(b));
  }

  /**
   * Writes a character to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param c the character to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(char c) throws IOException {
    println(String.valueOf(c));
  }

  /**
   * Writes an int to the client, followed by a carriage return-line feed (CRLF) character.
   *
   * @param i the int to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(int i) throws IOException {
    println(String.valueOf(i));
  }

  /**
   * Writes a <code>long</code> value to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param l the <code>long</code> value to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(long l) throws IOException {
    println(String.valueOf(l));
  }

  /**
   * Writes a <code>float</code> value to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param f the <code>float</code> value to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(float f) throws IOException {
    println(String.valueOf(f));
  }

  /**
   * Writes a <code>double</code> value to the client, followed by a carriage return-line feed (CRLF).
   *
   * @param d the <code>double</code> value to write to the client
   * @throws IOException if an input or output exception occurred
   */
  public void println(double d) throws IOException {
    println(String.valueOf(d));
  }

  /**
   * Returns true if data can be written without blocking else returns false.
   * <p>
   * If this method returns false and a {@link WriteListener} has been set with {@link #setWriteListener(WriteListener)},
   * then container will subsequently invoke {@link WriteListener#onWritePossible()} once a write operation becomes
   * possible without blocking. Other than the initial call, {@link WriteListener#onWritePossible()} will only be called
   * if and only if this method is called and returns false.
   *
   * @return <code>true</code> if data can be written without blocking, otherwise returns <code>false</code>.
   * @see WriteListener
   * @since Servlet 3.1
   */
  public abstract boolean isReady();

  /**
   * Instructs the <code>ServletOutputStream</code> to invoke the provided {@link WriteListener} when it is possible to
   * write
   *
   * @param writeListener the {@link WriteListener} that should be notified when it's possible to write
   * @throws IllegalStateException if one of the following conditions is true
   * <ul>
   * <li>the associated request is neither upgraded nor the async started
   * <li>setWriteListener is called more than once within the scope of the same request.
   * </ul>
   * @throws NullPointerException if writeListener is null
   * @since Servlet 3.1
   */
  public abstract void setWriteListener(WriteListener writeListener);

}
