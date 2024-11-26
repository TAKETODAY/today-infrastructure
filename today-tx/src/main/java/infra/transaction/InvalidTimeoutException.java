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

package infra.transaction;

/**
 * Exception that gets thrown when an invalid timeout is specified,
 * that is, the specified timeout valid is out of range or the
 * transaction manager implementation doesn't support timeouts.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-09 11:09
 */
public class InvalidTimeoutException extends TransactionUsageException {

  private final int timeout;

  /**
   * Constructor for InvalidTimeoutException.
   *
   * @param msg the detail message
   * @param timeout the invalid timeout value
   */
  public InvalidTimeoutException(String msg, int timeout) {
    super(msg);
    this.timeout = timeout;
  }

  /**
   * Return the invalid timeout value.
   */
  public int getTimeout() {
    return this.timeout;
  }

}
