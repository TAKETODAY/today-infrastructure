/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

import java.lang.reflect.UndeclaredThrowableException;

import cn.taketoday.lang.Autowired;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY <br>
 * 2018-12-30 21:02
 */
public class TransactionTemplate extends DefaultTransactionDefinition implements TransactionOperations {

  private static final Logger log = LoggerFactory.getLogger(TransactionTemplate.class);

  private final TransactionManager transactionManager;

  @Autowired
  public TransactionTemplate(@Autowired TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * Return the transaction management strategy to be used.
   */
  public final TransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    final TransactionManager transactionManager = getTransactionManager();
    final TransactionStatus status = transactionManager.getTransaction(this);

    try {
      final T result = action.doInTransaction(status);
      transactionManager.commit(status);
      return result;
    }
    catch (RuntimeException | Error ex) {
      // Transactional code threw application exception -> rollback
      rollbackOnException(transactionManager, status, ex);
      throw ex;
    }
    catch (Throwable ex) {
      // Transactional code threw unexpected exception -> rollback
      rollbackOnException(transactionManager, status, ex);
      throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
    }
  }

  /**
   * Perform a rollback, handling rollback exceptions properly.
   *
   * @param status
   *         object representing the transaction
   * @param ex
   *         the thrown application exception or error
   *
   * @throws TransactionException
   *         in case of a rollback error
   */
  protected void rollbackOnException(TransactionManager transactionManager, TransactionStatus status, Throwable ex)
          throws TransactionException //
  {

    log.debug("Initiating transaction rollback on application exception", ex);
    try {
      getTransactionManager().rollback(status);
    }
    catch (TransactionSystemException ex2) {
      log.error("Application exception overridden by rollback exception", ex);
      ex2.initApplicationException(ex);
      throw ex2;
    }
    catch (RuntimeException | Error ex2) {
      log.error("Application exception overridden by roll	back exception", ex);
      throw ex2;
    }
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (super.equals(other) //
            && (!(other instanceof TransactionTemplate) || getTransactionManager()
            == ((TransactionTemplate) other).getTransactionManager())));
  }

}
