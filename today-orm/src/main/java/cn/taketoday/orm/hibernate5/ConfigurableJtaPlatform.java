/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.orm.hibernate5;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.jta.UserTransactionAdapter;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

/**
 * Implementation of Hibernate 5's JtaPlatform SPI, exposing passed-in {@link TransactionManager},
 * {@link UserTransaction} and {@link TransactionSynchronizationRegistry} references.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
class ConfigurableJtaPlatform implements JtaPlatform {

  private final TransactionManager transactionManager;

  private final UserTransaction userTransaction;

  @Nullable
  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  /**
   * Create a new ConfigurableJtaPlatform instance with the given
   * JTA TransactionManager and optionally a given UserTransaction.
   *
   * @param tm the JTA TransactionManager reference (required)
   * @param ut the JTA UserTransaction reference (optional)
   * @param tsr the JTA 1.1 TransactionSynchronizationRegistry (optional)
   */
  public ConfigurableJtaPlatform(TransactionManager tm, @Nullable UserTransaction ut,
          @Nullable TransactionSynchronizationRegistry tsr) {

    Assert.notNull(tm, "TransactionManager reference is required");
    this.transactionManager = tm;
    this.userTransaction = (ut != null ? ut : new UserTransactionAdapter(tm));
    this.transactionSynchronizationRegistry = tsr;
  }

  @Override
  public TransactionManager retrieveTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public UserTransaction retrieveUserTransaction() {
    return this.userTransaction;
  }

  @Override
  public Object getTransactionIdentifier(Transaction transaction) {
    return transaction;
  }

  @Override
  public boolean canRegisterSynchronization() {
    try {
      return transactionManager.getStatus() == Status.STATUS_ACTIVE;
    }
    catch (SystemException ex) {
      throw new TransactionException("Could not determine JTA transaction status", ex);
    }
  }

  @Override
  public void registerSynchronization(Synchronization synchronization) {
    if (transactionSynchronizationRegistry != null) {
      transactionSynchronizationRegistry.registerInterposedSynchronization(synchronization);
    }
    else {
      try {
        transactionManager.getTransaction().registerSynchronization(synchronization);
      }
      catch (Exception ex) {
        throw new TransactionException("Could not access JTA Transaction to register synchronization", ex);
      }
    }
  }

  @Override
  public int getCurrentStatus() throws SystemException {
    return this.transactionManager.getStatus();
  }

}
