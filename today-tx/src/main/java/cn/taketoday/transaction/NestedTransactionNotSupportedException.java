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
 * Exception thrown when attempting to work with a nested transaction but nested
 * transactions are not supported by the underlying backend.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-11-09 19:36
 */
public class NestedTransactionNotSupportedException extends CannotCreateTransactionException {

  public NestedTransactionNotSupportedException(String msg) {
    super(msg);
  }

  public NestedTransactionNotSupportedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
