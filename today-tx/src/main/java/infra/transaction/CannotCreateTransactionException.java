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
 * Exception thrown when a transaction can't be created using an underlying
 * transaction API such as JTA.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-11-09 15:48
 */
public class CannotCreateTransactionException extends TransactionException {

  /**
   * Constructor for CannotCreateTransactionException.
   *
   * @param msg the detail message
   */
  public CannotCreateTransactionException(String msg) {
    super(msg);
  }

  /**
   * Constructor for CannotCreateTransactionException.
   *
   * @param msg the detail message
   * @param cause the root cause from the transaction API in use
   */
  public CannotCreateTransactionException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
