/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.transaction;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.TestContext;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionStatus;

/**
 * Transaction context for a specific {@link TestContext}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see infra.transaction.annotation.Transactional
 * @see TransactionalTestExecutionListener
 * @since 4.0
 */
class TransactionContext {

  private static final Logger logger = LoggerFactory.getLogger(TransactionContext.class);

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
