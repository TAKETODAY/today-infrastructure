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

package cn.taketoday.test.context.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;

/**
 * Transaction context for a specific {@link TestContext}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see cn.taketoday.transaction.annotation.Transactional
 * @see TransactionalTestExecutionListener
 * @since 4.0
 */
class TransactionContext {

  private static final Log logger = LogFactory.getLog(TransactionContext.class);

  private final TestContext testContext;

  private final TransactionDefinition transactionDefinition;

  private final PlatformTransactionManager transactionManager;

  private final boolean defaultRollback;

  private boolean flaggedForRollback;

  @Nullable
  private TransactionStatus transactionStatus;

  private final AtomicInteger transactionsStarted = new AtomicInteger();

  TransactionContext(TestContext testContext, PlatformTransactionManager transactionManager,
          TransactionDefinition transactionDefinition, boolean defaultRollback) {

    this.testContext = testContext;
    this.transactionManager = transactionManager;
    this.transactionDefinition = transactionDefinition;
    this.defaultRollback = defaultRollback;
    this.flaggedForRollback = defaultRollback;
  }

  @Nullable
  TransactionStatus getTransactionStatus() {
    return this.transactionStatus;
  }

  /**
   * Has the current transaction been flagged for rollback?
   * <p>In other words, should we roll back or commit the current transaction
   * upon completion of the current test?
   */
  boolean isFlaggedForRollback() {
    return this.flaggedForRollback;
  }

  void setFlaggedForRollback(boolean flaggedForRollback) {
    Assert.state(this.transactionStatus != null, () ->
            "Failed to set rollback flag - transaction does not exist: " + this.testContext);
    this.flaggedForRollback = flaggedForRollback;
  }

  /**
   * Start a new transaction for the configured test context.
   * <p>Only call this method if {@link #endTransaction} has been called or if no
   * transaction has been previously started.
   *
   * @throws TransactionException if starting the transaction fails
   */
  void startTransaction() {
    Assert.state(this.transactionStatus == null,
            "Cannot start a new transaction without ending the existing transaction first");

    this.flaggedForRollback = this.defaultRollback;
    this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
    int transactionsStarted = this.transactionsStarted.incrementAndGet();

    if (logger.isInfoEnabled()) {
      logger.info(String.format(
              "Began transaction (%s) for test context %s; transaction manager [%s]; rollback [%s]",
              transactionsStarted, this.testContext, this.transactionManager, this.flaggedForRollback));
    }
  }

  /**
   * Immediately force a <em>commit</em> or <em>rollback</em> of the transaction for the
   * configured test context, according to the {@linkplain #isFlaggedForRollback rollback flag}.
   */
  void endTransaction() {
    if (logger.isTraceEnabled()) {
      logger.trace(String.format(
              "Ending transaction for test context %s; transaction status [%s]; rollback [%s]",
              this.testContext, this.transactionStatus, this.flaggedForRollback));
    }
    Assert.state(this.transactionStatus != null,
            () -> "Failed to end transaction - transaction does not exist: " + this.testContext);

    try {
      if (this.flaggedForRollback) {
        this.transactionManager.rollback(this.transactionStatus);
      }
      else {
        this.transactionManager.commit(this.transactionStatus);
      }
    }
    finally {
      this.transactionStatus = null;
    }

    if (logger.isInfoEnabled()) {
      logger.info((this.flaggedForRollback ? "Rolled back" : "Committed") +
              " transaction for test: " + this.testContext);
    }
  }

}
