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

package cn.taketoday.transaction;

/**
 * Superclass for exceptions caused by inappropriate usage of
 * a Framework transaction API.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rod Johnson
 * @since 4.0 2021/12/10 21:33
 */
public class TransactionUsageException extends TransactionException {

  /**
   * Constructor for TransactionUsageException.
   *
   * @param msg the detail message
   */
  public TransactionUsageException(String msg) {
    super(msg);
  }

  /**
   * Constructor for TransactionUsageException.
   *
   * @param msg the detail message
   * @param cause the root cause from the transaction API in use
   */
  public TransactionUsageException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
