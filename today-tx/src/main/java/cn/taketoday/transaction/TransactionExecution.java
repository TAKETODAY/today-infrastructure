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

package cn.taketoday.transaction;

/**
 * Common representation of the current state of a transaction.
 * Serves as base interface for {@link TransactionStatus} as well as
 * {@link ReactiveTransaction}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/10 21:36
 */
public interface TransactionExecution {

  /**
   * Return whether the present transaction is new; otherwise participating
   * in an existing transaction, or potentially not running in an actual
   * transaction in the first place.
   */
  boolean isNewTransaction();

  /**
   * Set the transaction rollback-only. This instructs the transaction manager
   * that the only possible outcome of the transaction may be a rollback, as
   * alternative to throwing an exception which would in turn trigger a rollback.
   */
  void setRollbackOnly();

  /**
   * Return whether the transaction has been marked as rollback-only
   * (either by the application or by the transaction infrastructure).
   */
  boolean isRollbackOnly();

  /**
   * Return whether this transaction is completed, that is,
   * whether it has already been committed or rolled back.
   */
  boolean isCompleted();

}
