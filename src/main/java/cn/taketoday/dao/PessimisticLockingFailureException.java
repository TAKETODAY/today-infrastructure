/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.dao;

/**
 * Exception thrown on a pessimistic locking violation.
 * Thrown by  SQLException translation mechanism
 * if a corresponding database error is encountered.
 *
 * <p>Serves as superclass for more specific exceptions, like
 * CannotAcquireLockException and DeadlockLoserDataAccessException.
 *
 * @author Thomas Risberg
 * @see CannotAcquireLockException
 * @see DeadlockLoserDataAccessException
 * @see OptimisticLockingFailureException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PessimisticLockingFailureException extends ConcurrencyFailureException {

  /**
   * Constructor for PessimisticLockingFailureException.
   *
   * @param msg the detail message
   */
  public PessimisticLockingFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for PessimisticLockingFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public PessimisticLockingFailureException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
