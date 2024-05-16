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

package cn.taketoday.dao;

import cn.taketoday.lang.Nullable;

/**
 * Exception thrown on an optimistic locking violation.
 *
 * <p>This exception will be thrown either by O/R mapping tools
 * or by custom DAO implementations. Optimistic locking failure
 * is typically <i>not</i> detected by the database itself.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PessimisticLockingFailureException
 */
public class OptimisticLockingFailureException extends ConcurrencyFailureException {

  /**
   * Constructor for OptimisticLockingFailureException.
   *
   * @param msg the detail message
   */
  public OptimisticLockingFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for OptimisticLockingFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public OptimisticLockingFailureException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
