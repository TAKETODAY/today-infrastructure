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

package cn.taketoday.transaction.jta;

import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.support.SmartTransactionObject;
import cn.taketoday.transaction.support.TransactionSynchronizationUtils;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * JTA transaction object, representing a {@link UserTransaction}.
 * Used as transaction object by Framework's {@link JtaTransactionManager}.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @see JtaTransactionManager
 * @see UserTransaction
 * @since 4.0
 */
public class JtaTransactionObject implements SmartTransactionObject {

  private final UserTransaction userTransaction;

  boolean resetTransactionTimeout = false;

  /**
   * Create a new JtaTransactionObject for the given JTA UserTransaction.
   *
   * @param userTransaction the JTA UserTransaction for the current transaction
   * (either a shared object or retrieved through a fresh per-transaction lookup)
   */
  public JtaTransactionObject(UserTransaction userTransaction) {
    this.userTransaction = userTransaction;
  }

  /**
   * Return the JTA UserTransaction object for the current transaction.
   */
  public final UserTransaction getUserTransaction() {
    return this.userTransaction;
  }

  /**
   * This implementation checks the UserTransaction's rollback-only flag.
   */
  @Override
  public boolean isRollbackOnly() {
    try {
      int jtaStatus = this.userTransaction.getStatus();
      return (jtaStatus == Status.STATUS_MARKED_ROLLBACK || jtaStatus == Status.STATUS_ROLLEDBACK);
    }
    catch (SystemException ex) {
      throw new TransactionSystemException("JTA failure on getStatus", ex);
    }
  }

  /**
   * This implementation triggers flush callbacks,
   * assuming that they will flush all affected ORM sessions.
   *
   * @see cn.taketoday.transaction.support.TransactionSynchronization#flush()
   */
  @Override
  public void flush() {
    TransactionSynchronizationUtils.triggerFlush();
  }

}
