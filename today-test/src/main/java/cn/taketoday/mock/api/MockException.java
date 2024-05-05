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

/**
 * Defines a general exception a servlet can throw when it encounters difficulty.
 *
 * @author Various
 */
public class MockException extends Exception {

  private static final long serialVersionUID = 4221302886851315160L;

  private Throwable rootCause;

  /**
   * Constructs a new servlet exception.
   */
  public MockException() {
    super();
  }

  /**
   * Constructs a new servlet exception with the specified message. The message can be written to the server log and/or
   * displayed for the user.
   *
   * @param message a <code>String</code> specifying the text of the exception message
   */
  public MockException(String message) {
    super(message);
  }

  /**
   * Constructs a new servlet exception when the servlet needs to throw an exception and include a message about the "root
   * cause" exception that interfered with its normal operation, including a description message.
   *
   * @param message a <code>String</code> containing the text of the exception message
   * @param rootCause the <code>Throwable</code> exception that interfered with the servlet's normal operation, making
   * this servlet exception necessary
   */
  public MockException(String message, Throwable rootCause) {
    super(message, rootCause);
    this.rootCause = rootCause;
  }

  /**
   * Constructs a new servlet exception when the servlet needs to throw an exception and include a message about the "root
   * cause" exception that interfered with its normal operation. The exception's message is based on the localized message
   * of the underlying exception.
   *
   * <p>
   * This method calls the <code>getLocalizedMessage</code> method on the <code>Throwable</code> exception to get a
   * localized exception message. When subclassing <code>ServletException</code>, this method can be overridden to create
   * an exception message designed for a specific locale.
   *
   * @param rootCause the <code>Throwable</code> exception that interfered with the servlet's normal operation, making the
   * servlet exception necessary
   */
  public MockException(Throwable rootCause) {
    super(rootCause);
    this.rootCause = rootCause;
  }

  /**
   * Returns the exception that caused this servlet exception.
   *
   * @return the <code>Throwable</code> that caused this servlet exception
   */
  public Throwable getRootCause() {
    return rootCause;
  }
}
