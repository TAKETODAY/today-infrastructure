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
 * Exception thrown when the existence or non-existence of a transaction
 * amounts to an illegal state according to the transaction propagation
 * behavior that applies.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/10 21:33
 */
public class IllegalTransactionStateException extends TransactionUsageException {

  /**
   * Constructor for IllegalTransactionStateException.
   *
   * @param msg the detail message
   */
  public IllegalTransactionStateException(String msg) {
    super(msg);
  }

  /**
   * Constructor for IllegalTransactionStateException.
   *
   * @param msg the detail message
   * @param cause the root cause from the transaction API in use
   */
  public IllegalTransactionStateException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
