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

import cn.taketoday.lang.Nullable;

/**
 * Exception thrown on concurrency failure.
 *
 * <p>This exception should be subclassed to indicate the type of failure:
 * optimistic locking, failure to acquire lock, etc.
 *
 * @author Thomas Risberg
 * @see OptimisticLockingFailureException
 * @see PessimisticLockingFailureException
 * @see CannotAcquireLockException
 * @see DeadlockLoserDataAccessException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ConcurrencyFailureException extends TransientDataAccessException {

  /**
   * Constructor for ConcurrencyFailureException.
   *
   * @param msg the detail message
   */
  public ConcurrencyFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for ConcurrencyFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public ConcurrencyFailureException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
