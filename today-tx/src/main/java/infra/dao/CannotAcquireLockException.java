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

package infra.dao;

/**
 * Exception thrown on failure to acquire a lock during an update,
 * for example during a "select for update" statement.
 *
 * <p>Consider handling the general {@link PessimisticLockingFailureException}
 * instead, semantically including a wider range of locking-related failures.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class CannotAcquireLockException extends PessimisticLockingFailureException {

  /**
   * Constructor for CannotAcquireLockException.
   *
   * @param msg the detail message
   */
  public CannotAcquireLockException(String msg) {
    super(msg);
  }

  /**
   * Constructor for CannotAcquireLockException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public CannotAcquireLockException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
