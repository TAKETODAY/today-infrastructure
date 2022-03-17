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

package cn.taketoday.transaction.support;

import java.io.Flushable;

/**
 * Interface to be implemented by transaction objects that are able to
 * return an internal rollback-only marker, typically from a another
 * transaction that has participated and marked it as rollback-only.
 *
 * <p>Autodetected by DefaultTransactionStatus, to always return a
 * current rollbackOnly flag even if not resulting from the current
 * TransactionStatus.
 *
 * @author Juergen Hoeller
 * @see DefaultTransactionStatus#isRollbackOnly
 * @since 4.0
 */
public interface SmartTransactionObject extends Flushable {

  /**
   * Return whether the transaction is internally marked as rollback-only.
   * Can, for example, check the JTA UserTransaction.
   *
   * @see jakarta.transaction.UserTransaction#getStatus
   * @see jakarta.transaction.Status#STATUS_MARKED_ROLLBACK
   */
  boolean isRollbackOnly();

  /**
   * Flush the underlying sessions to the datastore, if applicable:
   * for example, all affected Hibernate/JPA sessions.
   */
  @Override
  void flush();

}
