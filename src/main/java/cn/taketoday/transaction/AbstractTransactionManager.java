/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;

import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_DEFAULT;

/**
 * @author TODAY <br>
 * 2018-11-06 22:51
 */
public abstract class AbstractTransactionManager implements TransactionManager, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(AbstractTransactionManager.class);
  public static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Always activate transaction synchronization, even for "empty" transactions
   * that result from PROPAGATION_SUPPORTS with no existing backend transaction.
   *
   * @see TransactionDefinition#PROPAGATION_SUPPORTS
   * @see TransactionDefinition#PROPAGATION_NOT_SUPPORTED
   * @see TransactionDefinition#PROPAGATION_NEVER
   */
  public static final int SYNCHRONIZATION_ALWAYS = 0;

  /**
   * Activate transaction synchronization only for actual transactions, that is,
   * not for empty ones that result from PROPAGATION_SUPPORTS with no existing
   * backend transaction.
   *
   * @see TransactionDefinition#PROPAGATION_REQUIRED
   * @see TransactionDefinition#PROPAGATION_MANDATORY
   * @see TransactionDefinition#PROPAGATION_REQUIRES_NEW
   */
  public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

  /**
   * Never active transaction synchronization, not even for actual transactions.
   */
  public static final int SYNCHRONIZATION_NEVER = 2;

  private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;
  private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

  private boolean nestedTransactionAllowed = false;
  private boolean rollbackOnCommitFailure = false;
  private boolean validateExistingTransaction = false;
  private boolean failEarlyOnGlobalRollbackOnly = false;
  private boolean globalRollbackOnParticipationFailure = true;

  /**
   * Set when this transaction manager should activate the thread-bound
   * transaction synchronization support. Default is "always".
   * <p>
   * Note that transaction synchronization isn't supported for multiple concurrent
   * transactions by different transaction managers. Only one transaction manager
   * is allowed to activate it at any time.
   *
   * @see #SYNCHRONIZATION_ALWAYS
   * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
   * @see #SYNCHRONIZATION_NEVER
   * @see SynchronizationManager
   * @see TransactionSynchronization
   */
  public final void setTransactionSynchronization(int transactionSynchronization) {
    this.transactionSynchronization = transactionSynchronization;
  }

  /**
   * Return if this transaction manager should activate the thread-bound
   * transaction synchronization support.
   */
  public final int getTransactionSynchronization() {
    return this.transactionSynchronization;
  }

  /**
   * Specify the default timeout that this transaction manager should apply if
   * there is no timeout specified at the transaction level, in seconds.
   * <p>
   * Default is the underlying transaction infrastructure's default timeout, e.g.
   * typically 30 seconds in case of a JTA provider, indicated by the
   * {@code TransactionDefinition.TIMEOUT_DEFAULT} value.
   *
   * @see TransactionDefinition#TIMEOUT_DEFAULT
   */
  public void setDefaultTimeout(int defaultTimeout) {
    if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
      throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
    }
    this.defaultTimeout = defaultTimeout;
  }

  /**
   * Return the default timeout that this transaction manager should apply if
   * there is no timeout specified at the transaction level, in seconds.
   * <p>
   * Returns {@code TransactionDefinition.TIMEOUT_DEFAULT} to indicate the
   * underlying transaction infrastructure's default timeout.
   */
  public int getDefaultTimeout() {
    return this.defaultTimeout;
  }

  /**
   * Set whether nested transactions are allowed. Default is "false".
   * <p>
   * Typically initialized with an appropriate default by the concrete transaction
   * manager subclass.
   */
  public void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
    this.nestedTransactionAllowed = nestedTransactionAllowed;
  }

  /**
   * Return whether nested transactions are allowed.
   */
  public boolean isNestedTransactionAllowed() {
    return this.nestedTransactionAllowed;
  }

  /**
   * Set whether existing transactions should be validated before participating in
   * them.
   * <p>
   * When participating in an existing transaction (e.g. with PROPAGATION_REQUIRED
   * or PROPAGATION_SUPPORTS encountering an existing transaction), this outer
   * transaction's characteristics will apply even to the inner transaction scope.
   * Validation will detect incompatible isolation level and read-only settings on
   * the inner transaction definition and reject participation accordingly through
   * throwing a corresponding exception.
   * <p>
   * Default is "false", leniently ignoring inner transaction settings, simply
   * overriding them with the outer transaction's characteristics. Switch this
   * flag to "true" in order to enforce strict validation.
   */
  public void setValidateExistingTransaction(boolean validateExistingTransaction) {
    this.validateExistingTransaction = validateExistingTransaction;
  }

  /**
   * Return whether existing transactions should be validated before participating
   * in them.
   */
  public boolean isValidateExistingTransaction() {
    return this.validateExistingTransaction;
  }

  /**
   * Set whether to globally mark an existing transaction as rollback-only after a
   * participating transaction failed.
   * <p>
   * Default is "true": If a participating transaction (e.g. with
   * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
   * transaction) fails, the transaction will be globally marked as rollback-only.
   * The only possible outcome of such a transaction is a rollback: The
   * transaction originator <i>cannot</i> make the transaction commit anymore.
   * <p>
   * Switch this to "false" to let the transaction originator make the rollback
   * decision. If a participating transaction fails with an exception, the caller
   * can still decide to continue with a different path within the transaction.
   * However, note that this will only work as long as all participating resources
   * are capable of continuing towards a transaction commit even after a data
   * access failure: This is generally not the case for a Hibernate Session, for
   * example; neither is it for a sequence of JDBC insert/update/delete
   * operations.
   * <p>
   * <b>Note:</b>This flag only applies to an explicit rollback attempt for a
   * subtransaction, typically caused by an exception thrown by a data access
   * operation (where TransactionInterceptor will trigger a
   * {@code PlatformTransactionManager.rollback()} call according to a rollback
   * rule). If the flag is off, the caller can handle the exception and decide on
   * a rollback, independent of the rollback rules of the subtransaction. This
   * flag does, however, <i>not</i> apply to explicit {@code setRollbackOnly}
   * calls on a {@code TransactionStatus}, which will always cause an eventual
   * global rollback (as it might not throw an exception after the rollback-only
   * call).
   * <p>
   * The recommended solution for handling failure of a subtransaction is a
   * "nested transaction", where the global transaction can be rolled back to a
   * savepoint taken at the beginning of the subtransaction. PROPAGATION_NESTED
   * provides exactly those semantics; however, it will only work when nested
   * transaction support is available. This is the case with
   * DataSourceTransactionManager, but not with JtaTransactionManager.
   *
   * @see #setNestedTransactionAllowed
   */
  public void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
    this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
  }

  /**
   * Return whether to globally mark an existing transaction as rollback-only
   * after a participating transaction failed.
   */
  public boolean isGlobalRollbackOnParticipationFailure() {
    return this.globalRollbackOnParticipationFailure;
  }

  /**
   * Set whether to fail early in case of the transaction being globally marked as
   * rollback-only.
   * <p>
   * Default is "false", only causing an UnexpectedRollbackException at the
   * outermost transaction boundary. Switch this flag on to cause an
   * UnexpectedRollbackException as early as the global rollback-only marker has
   * been first detected, even from within an inner transaction boundary.
   *
   * @see UnexpectedRollbackException
   */
  public void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
    this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
  }

  /**
   * Return whether to fail early in case of the transaction being globally marked
   * as rollback-only.
   */
  public boolean isFailEarlyOnGlobalRollbackOnly() {
    return this.failEarlyOnGlobalRollbackOnly;
  }

  /**
   * Set whether {@code doRollback} should be performed on failure of the
   * {@code doCommit} call. Typically not necessary and thus to be avoided, as it
   * can potentially override the commit exception with a subsequent rollback
   * exception.
   * <p>
   * Default is "false".
   *
   * @see #doCommit
   * @see #doRollback
   */
  public void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
    this.rollbackOnCommitFailure = rollbackOnCommitFailure;
  }

  /**
   * Return whether {@code doRollback} should be performed on failure of the
   * {@code doCommit} call.
   */
  public boolean isRollbackOnCommitFailure() {
    return this.rollbackOnCommitFailure;
  }

  /**
   * This implementation handles propagation behavior. Delegates to
   * {@code doGetTransaction}, {@code isExistingTransaction} and {@code doBegin}.
   *
   * @see #doGetTransaction
   * @see #isExistingTransaction
   * @see #doBegin
   */
  @Override
  public TransactionStatus getTransaction(TransactionDefinition def) throws TransactionException {
    Object transaction = doGetTransaction();

    if (isExistingTransaction(transaction)) {
      // Existing transaction found -> check propagation behavior to find out how to behave.
      return handleExistingTransaction(def, transaction);
    }

    // No existing transaction found -> check propagation behavior to find out how to proceed.
    switch (def.getPropagationBehavior()) {
      case TransactionDefinition.PROPAGATION_MANDATORY: {
        throw new IllegalStateException("No existing transaction found for transaction marked with propagation 'mandatory'");
      }
      case TransactionDefinition.PROPAGATION_NESTED:
      case TransactionDefinition.PROPAGATION_REQUIRED:
      case TransactionDefinition.PROPAGATION_REQUIRES_NEW: {
        final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();
        final SuspendedResourcesHolder res = suspend(metaData, null);
        if (debugEnabled) {
          log.debug("Creating new transaction with name [{}]: {}", def.getName(), def);
        }
        try {
          boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER) && !metaData.isActive();
          final DefaultTransactionStatus status = newTransactionStatus(def, transaction, true, newSynchronization, res);
          doBegin(metaData, transaction, def);
          prepareSynchronization(metaData, status, def);
          return status;
        }
        catch (RuntimeException | Error ex) {
          resume(metaData, null, res);
          throw ex;
        }
      }
    }

    // Create "empty" transaction: no actual transaction, but potentially synchronization.
    if (def.getIsolationLevel() != ISOLATION_DEFAULT) {
      log.warn("Custom isolation level specified but no actual transaction initiated; isolation level will effectively be ignored: {}",
               def);
    }
    boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
    return prepareTransactionStatus(SynchronizationManager.getMetaData(), def, null, true, newSynchronization, null);
  }

  /**
   * Create a TransactionStatus for an existing transaction.
   */
  protected TransactionStatus handleExistingTransaction(final TransactionDefinition def,
                                                        final Object transaction) throws TransactionException //
  {
    final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();

    switch (def.getPropagationBehavior()) {
      case TransactionDefinition.PROPAGATION_NEVER:
        throw new IllegalStateException("Existing transaction found for transaction marked with propagation 'never'");
      case TransactionDefinition.PROPAGATION_NOT_SUPPORTED: {
        if (debugEnabled) {
          log.debug("Suspending current transaction");
        }
        final Object suspendedResources = suspend(metaData, transaction);
        boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
        return prepareTransactionStatus(metaData, def, null, false, newSynchronization, suspendedResources);
      }
      case TransactionDefinition.PROPAGATION_REQUIRES_NEW: {

        if (debugEnabled) {
          log.debug("Suspending current transaction, creating new transaction with name [{}]",
                    def.getName());
        }
        final SuspendedResourcesHolder res = suspend(metaData, transaction);

        try {
          boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER) && !metaData.isActive();
          final DefaultTransactionStatus status = //
                  newTransactionStatus(def, transaction, true, newSynchronization, res);

          doBegin(metaData, transaction, def);
          prepareSynchronization(metaData, status, def);
          return status;
        }
        catch (RuntimeException | Error beginEx) {
          resumeAfterBeginException(metaData, transaction, res, beginEx);
          throw beginEx;
        }
      }
      case TransactionDefinition.PROPAGATION_NESTED: {
        if (!isNestedTransactionAllowed()) {
          throw new NestedTransactionNotSupportedException("Transaction manager does not allow nested transactions by default");
        }

        if (debugEnabled) {
          log.debug("Creating nested transaction with name [{}]", def.getName());
        }

        if (useSavepointForNestedTransaction()) { //save point
          // Create savepoint within existing managed transaction,
          // through the SavepointManager API implemented by TransactionStatus.
          // Usually uses JDBC 3.0 savepoints. Never activates synchronization.
          DefaultTransactionStatus status = //
                  prepareTransactionStatus(metaData, def, transaction, false, false, null);

          status.createAndHoldSavepoint();
          return status;
        }

        // Nested transaction through nested begin and commit/rollback calls.
        // Usually only for JTA: synchronization might get activated here
        // in case of a pre-existing JTA transaction.
        boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER) && !metaData.isActive();

        DefaultTransactionStatus status = //
                newTransactionStatus(def, transaction, true, newSynchronization, null);

        doBegin(metaData, transaction, def);
        prepareSynchronization(metaData, status, def);
        return status;
      }
    }
    // Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
    if (debugEnabled) {
      log.debug("Participating in existing transaction");
    }

    if (isValidateExistingTransaction()) {
      final int isolationLevel = def.getIsolationLevel();
      if (isolationLevel != ISOLATION_DEFAULT) {
        final Integer currentIsolationLevel = metaData.getIsolationLevel();
        if (currentIsolationLevel == null || currentIsolationLevel != isolationLevel) {
          throw new IllegalStateException("Participating transaction with definition [" + def
                                                  + "] specifies isolation level which is incompatible with existing transaction: "
                                                  + (currentIsolationLevel != null ? currentIsolationLevel : "(unknown)"));
        }
      }
      if (!def.isReadOnly() && metaData.isReadOnly()) {
        throw new IllegalStateException("Participating transaction with definition [" + def
                                                + "] is not marked as read-only but existing transaction is");
      }
    }
    boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
    return prepareTransactionStatus(metaData, def, transaction, false, newSynchronization, null);
  }

  /**
   * Create a new TransactionStatus for the given arguments, also initializing
   * transaction synchronization as appropriate.
   *
   * @see #newTransactionStatus
   * @see #prepareTransactionStatus
   */
  protected DefaultTransactionStatus prepareTransactionStatus(final SynchronizationMetaData metaData,
                                                              final TransactionDefinition definition,
                                                              final Object transaction,
                                                              final boolean newTransaction,
                                                              final boolean newSynchronization,
                                                              final Object suspendedResources) {

    final DefaultTransactionStatus status = newTransactionStatus(definition,
                                                                 transaction,
                                                                 newTransaction,
                                                                 newSynchronization && !metaData.isActive(),
                                                                 suspendedResources);
    prepareSynchronization(metaData, status, definition);
    return status;
  }

  /**
   * Create a TransactionStatus instance for the given arguments.
   */
  protected DefaultTransactionStatus newTransactionStatus(final TransactionDefinition definition,
                                                          final Object transaction,
                                                          final boolean newTransaction,
                                                          final boolean newSynchronization,
                                                          final Object suspendedResources) {
    return new DefaultTransactionStatus(transaction,
                                        newTransaction,
                                        newSynchronization,
                                        definition.isReadOnly(),
                                        suspendedResources);
  }

  /**
   * Initialize transaction synchronization as appropriate.
   */
  protected void prepareSynchronization(final SynchronizationMetaData metaData,
                                        final DefaultTransactionStatus status, final TransactionDefinition definition) {

    if (status.isNewSynchronization()) {

      metaData.setActualActive(status.hasTransaction());
      final int isolationLevel = definition.getIsolationLevel();
      metaData.setIsolationLevel(isolationLevel != ISOLATION_DEFAULT ? isolationLevel : null);

      metaData.setName(definition.getName());
      metaData.setReadOnly(definition.isReadOnly());

      metaData.initSynchronization();
    }
  }

  /**
   * Determine the actual timeout to use for the given definition. Will fall back
   * to this manager's default timeout if the transaction definition doesn't
   * specify a non-default value.
   *
   * @param definition
   *            the transaction definition
   * @return the actual timeout to use
   * @see TransactionDefinition#getTimeout()
   * @see #setDefaultTimeout
   */
  protected int determineTimeout(TransactionDefinition definition) {
    if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
      return definition.getTimeout();
    }
    return this.defaultTimeout;
  }

  /**
   * Suspend the given transaction. Suspends transaction synchronization first,
   * then delegates to the {@code doSuspend} template method.
   *
   * @param transaction
   *            the current transaction object (or {@code null} to just suspend
   *            active synchronizations, if any)
   * @return an object that holds suspended resources (or {@code null} if neither
   *         transaction nor synchronization active)
   * @see #doSuspend
   * @see #resume
   */
  protected SuspendedResourcesHolder suspend(final SynchronizationMetaData metaData, Object transaction) throws TransactionException {

    if (metaData.isActive()) {
      List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization(metaData);
      try {

        Object suspendedResources = null;
        if (transaction != null) {
          suspendedResources = doSuspend(metaData, transaction);
        }
        String name = metaData.getName();
        metaData.setName(null);

        boolean readOnly = metaData.isReadOnly();
        metaData.setReadOnly(false);

        Integer isolationLevel = metaData.getIsolationLevel();
        metaData.setIsolationLevel(null);

        boolean wasActive = metaData.isActualActive();
        metaData.setActualActive(false);

        return new SuspendedResourcesHolder(name,
                                            readOnly,
                                            wasActive,
                                            isolationLevel,
                                            suspendedResources,
                                            suspendedSynchronizations);
      }
      catch (RuntimeException | Error ex) {
        // doSuspend failed - original transaction is still active...
        doResumeSynchronization(metaData, suspendedSynchronizations);
        throw ex;
      }
    }
    else if (transaction != null) {
      // Transaction active but no synchronization active.
      final Object suspendedResources = doSuspend(metaData, transaction);
      return new SuspendedResourcesHolder(suspendedResources);
    }
    // Neither transaction nor synchronization active.
    return null;
  }

  /**
   * Resume the given transaction. Delegates to the {@code doResume} template
   * method first, then resuming transaction synchronization.
   *
   * @param transaction
   *            the current transaction object
   * @param resourcesHolder
   *            the object that holds suspended resources, as returned by
   *            {@code suspend} (or {@code null} to just resume synchronizations,
   *            if any)
   * @see #doResume
   * @see #suspend
   */
  protected void resume(final SynchronizationMetaData metaData,
                        final Object transaction,
                        final SuspendedResourcesHolder resourcesHolder) throws TransactionException {

    if (resourcesHolder != null) {
      Object suspendedResources = resourcesHolder.suspendedResources;
      if (suspendedResources != null) {
        doResume(metaData, transaction, suspendedResources);
      }
      List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
      if (suspendedSynchronizations != null) {
        metaData.setName(resourcesHolder.name);
        metaData.setReadOnly(resourcesHolder.readOnly);
        metaData.setActualActive(resourcesHolder.wasActive);
        metaData.setIsolationLevel(resourcesHolder.isolationLevel);

//                SynchronizationManager.setTransactionName(metaData, resourcesHolder.name);
//                SynchronizationManager.setTransactionReadOnly(metaData, resourcesHolder.readOnly);
//                SynchronizationManager.setActualTransactionActive(metaData, resourcesHolder.wasActive);
//                SynchronizationManager.setTransactionIsolationLevel(metaData, resourcesHolder.isolationLevel);

        doResumeSynchronization(metaData, suspendedSynchronizations);
      }
    }
  }

  /**
   * Resume outer transaction after inner transaction begin failed.
   */
  private void resumeAfterBeginException(final SynchronizationMetaData metaData, Object transaction, //
                                         SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

    try {
      resume(metaData, transaction, suspendedResources);
    }
    catch (TransactionException resumeEx) {
      log.error("Inner transaction begin exception overridden by outer transaction resume exception", beginEx);
      throw resumeEx;
    }
  }

  /**
   * Suspend all current synchronizations and deactivate transaction
   * synchronization for the current thread.
   *
   * @return the List of suspended TransactionSynchronization objects
   */
  private List<TransactionSynchronization> doSuspendSynchronization(final SynchronizationMetaData metaData) {

    final List<TransactionSynchronization> ret = metaData.getSynchronizations();
    for (TransactionSynchronization synchronization : ret) {
      synchronization.suspend(metaData);
    }
    metaData.clearSynchronization();
    return ret;
  }

  /**
   * Reactivate transaction synchronization for the current thread and resume all
   * given synchronizations.
   *
   * @param suspendedSynchronizations
   *            List of TransactionSynchronization objects
   */
  private void doResumeSynchronization(final SynchronizationMetaData metaData,
                                       final List<TransactionSynchronization> suspendedSynchronizations) {

    metaData.initSynchronization();

    for (final TransactionSynchronization synchronization : suspendedSynchronizations) {
      synchronization.resume(metaData);
      metaData.registerSynchronization(synchronization);
    }
  }

  @Override
  public void commit(TransactionStatus status) throws TransactionException {
    if (status.isCompleted()) {
      throw new IllegalStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
    }
    DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
    if (defStatus.isLocalRollbackOnly()) {
      if (debugEnabled) {
        log.debug("Transactional code has requested rollback");
      }
      processRollback(defStatus, false);
      return;
    }

    if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
      if (debugEnabled) {
        log.debug("Global transaction is marked as rollback-only but transactional code requested commit");
      }
      processRollback(defStatus, true);
      return;
    }

    processCommit(defStatus);
  }

  /**
   * Process an actual commit. Rollback-only flags have already been checked and
   * applied.
   *
   * @param status
   *            object representing the transaction
   * @throws TransactionException
   *             in case of commit failure
   */
  private void processCommit(DefaultTransactionStatus status) throws TransactionException {

    final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();
    try {
      boolean beforeCompletionInvoked = false;
      try {
        boolean unexpectedRollback = false;
        prepareForCommit(metaData, status);
        triggerBeforeCommit(metaData, status);
        triggerBeforeCompletion(metaData, status);
        beforeCompletionInvoked = true;

        if (status.hasSavepoint()) {
          if (debugEnabled) {
            log.debug("Releasing transaction savepoint");
          }
          unexpectedRollback = status.isGlobalRollbackOnly();
          status.releaseHeldSavepoint();
        }
        else if (status.isNewTransaction()) {
          if (debugEnabled) {
            log.debug("Initiating transaction commit");
          }
          unexpectedRollback = status.isGlobalRollbackOnly();
          doCommit(metaData, status);
        }
        else if (isFailEarlyOnGlobalRollbackOnly()) {
          unexpectedRollback = status.isGlobalRollbackOnly();
        }
        // Throw UnexpectedRollbackException if we have a global rollback-only
        // marker but still didn't get a corresponding exception from commit.
        if (unexpectedRollback) {
          throw new UnexpectedRollbackException("Transaction silently rolled back because it has been marked as rollback-only");
        }
      }
      catch (UnexpectedRollbackException ex) {
        // can only be caused by doCommit
        triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_ROLLED_BACK);
        throw ex;
      }
      catch (TransactionException ex) {
        // can only be caused by doCommit
        if (isRollbackOnCommitFailure()) {
          doRollbackOnCommitException(metaData, status, ex);
        }
        else {
          triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_UNKNOWN);
        }
        throw ex;
      }
      catch (RuntimeException | Error ex) {
        if (!beforeCompletionInvoked) {
          triggerBeforeCompletion(metaData, status);
        }
        doRollbackOnCommitException(metaData, status, ex);
        throw ex;
      }
      // Trigger afterCommit callbacks, with an exception thrown there
      // propagated to callers but the transaction still considered as committed.
      try {
        triggerAfterCommit(metaData, status);
      }
      finally {
        triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_COMMITTED);
      }
    }
    finally {
      cleanupAfterCompletion(metaData, status);
    }
  }

  /**
   * This implementation of rollback handles participating in existing
   * transactions. Delegates to {@code doRollback} and {@code doSetRollbackOnly}.
   *
   * @see #doRollback
   * @see #doSetRollbackOnly
   */
  @Override
  public void rollback(TransactionStatus status) throws TransactionException {
    if (status.isCompleted()) {
      throw new IllegalStateException("Transaction is already completed - do not call commit or rollback more than once per transaction");
    }
    processRollback((DefaultTransactionStatus) status, false);
  }

  /**
   * Process an actual rollback. The completed flag has already been checked.
   *
   * @param status
   *            object representing the transaction
   * @throws TransactionException
   *             in case of rollback failure
   */
  private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
    final SynchronizationMetaData metaData = SynchronizationManager.getMetaData();

    try {
      boolean unexpectedRollback = unexpected;
      try {
        triggerBeforeCompletion(metaData, status);

        if (status.hasSavepoint()) {
          if (debugEnabled) {
            log.debug("Rolling back transaction to savepoint");
          }
          status.rollbackToHeldSavepoint();
        }
        else if (status.isNewTransaction()) {
          if (debugEnabled) {
            log.debug("Initiating transaction rollback");
          }
          doRollback(metaData, status);
        }
        else {
          // Participating in larger transaction
          if (status.hasTransaction()) {
            if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
//							log.debug( "Participating transaction failed - marking existing transaction as rollback-only");
              doSetRollbackOnly(metaData, status);
            }
            else if (debugEnabled) {
              log.debug("Participating transaction failed - letting transaction originator decide on rollback");
            }
          }
          else if (debugEnabled) {
            log.debug("Should roll back transaction but cannot - no transaction available");
          }
          // Unexpected rollback only matters here if we're asked to fail early
          if (!isFailEarlyOnGlobalRollbackOnly()) {
            unexpectedRollback = false;
          }
        }
      }
      catch (RuntimeException | Error ex) {
        triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_UNKNOWN);
        throw ex;
      }

      triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_ROLLED_BACK);

      // Raise UnexpectedRollbackException if we had a global rollback-only marker
      if (unexpectedRollback) {
        throw new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only");
      }
    }
    finally {
      cleanupAfterCompletion(metaData, status);
    }
  }

  /**
   * Invoke {@code doRollback}, handling rollback exceptions properly.
   *
   * @param status
   *            object representing the transaction
   * @param ex
   *            the thrown application exception or error
   * @throws TransactionException
   *             in case of rollback failure
   * @see #doRollback
   */
  private void doRollbackOnCommitException(final SynchronizationMetaData metaData,
                                           final DefaultTransactionStatus status, final Throwable ex)
          throws TransactionException //
  {
    try {
      if (status.isNewTransaction()) {
        if (debugEnabled) {
          log.debug("Initiating transaction rollback after commit exception", ex);
        }
        doRollback(metaData, status);
      }
      else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
        if (debugEnabled) {
          log.debug("Marking existing transaction as rollback-only after commit exception", ex);
        }
        doSetRollbackOnly(metaData, status);
      }
    }
    catch (RuntimeException | Error rbex) {
      triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_UNKNOWN);
      log.error("Commit exception overridden by rollback exception", ex);
      throw rbex;
    }
    triggerAfterCompletion(metaData, status, TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  /**
   * Trigger {@code beforeCommit} callbacks.
   *
   * @param status
   *            object representing the transaction
   */
  protected void triggerBeforeCommit(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {
    if (status.isNewSynchronization()) {
      if (log.isTraceEnabled()) {
        log.trace("Triggering beforeCommit synchronization");
      }
      metaData.triggerBeforeCommit(status.isReadOnly());
    }
  }

  /**
   * Trigger {@code beforeCompletion} callbacks.
   *
   * @param status
   *            object representing the transaction
   */
  protected void triggerBeforeCompletion(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {
    if (status.isNewSynchronization()) {
      if (log.isTraceEnabled()) {
        log.trace("Triggering beforeCompletion synchronization");
      }
      metaData.triggerBeforeCompletion();
    }
  }

  /**
   * Trigger {@code afterCommit} callbacks.
   *
   * @param status
   *            object representing the transaction
   */
  private void triggerAfterCommit(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {
    if (status.isNewSynchronization()) {
      if (log.isTraceEnabled()) {
        log.trace("Triggering afterCommit synchronization");
      }
      metaData.triggerAfterCommit();
    }
  }

  /**
   * Trigger {@code afterCompletion} callbacks.
   *
   * @param status
   *            object representing the transaction
   * @param completionStatus
   *            completion status according to TransactionSynchronization
   *            constants
   */
  private void triggerAfterCompletion(final SynchronizationMetaData metaData,
                                      final DefaultTransactionStatus status, final int completionStatus) {

    if (status.isNewSynchronization()) {

      final List<TransactionSynchronization> synchronizations = metaData.getSynchronizations();
      metaData.clearSynchronization();

      if (!status.hasTransaction() || status.isNewTransaction()) {
        if (log.isTraceEnabled()) {
          log.trace("Triggering afterCompletion synchronization");
        }
        // No transaction or new transaction for the current scope ->
        // invoke the afterCompletion callbacks immediately
        invokeAfterCompletion(metaData, synchronizations, completionStatus);
      }
      else if (!synchronizations.isEmpty()) {
        // Existing transaction that we participate in, controlled outside
        // of the scope of this transaction manager -> try to register
        // an afterCompletion callback with the existing (JTA) transaction.
        registerAfterCompletionWithExistingTransaction(metaData, status.getTransaction(), synchronizations);
      }
    }
  }

  /**
   * Actually invoke the {@code afterCompletion} methods of the given
   * TransactionSynchronization objects.
   * <p>
   * To be called by this abstract manager itself, or by special implementations
   * of the {@code registerAfterCompletionWithExistingTransaction} callback.
   *
   * @param syncs
   *            List of TransactionSynchronization objects
   * @param completionStatus
   *            the completion status according to the constants in the
   *            TransactionSynchronization interface
   * @see TransactionSynchronization#STATUS_COMMITTED
   * @see TransactionSynchronization#STATUS_ROLLED_BACK
   * @see TransactionSynchronization#STATUS_UNKNOWN
   */
  protected void invokeAfterCompletion(final SynchronizationMetaData metaData,
                                       final List<TransactionSynchronization> syncs, final int completionStatus) {
    metaData.invokeAfterCompletion(syncs, completionStatus);
  }

  /**
   * Clean up after completion, clearing synchronization if necessary, and
   * invoking doCleanupAfterCompletion.
   *
   * @param status
   *            object representing the transaction
   * @see #doCleanupAfterCompletion
   */
  private void cleanupAfterCompletion(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {

    status.setCompleted();

    if (status.isNewSynchronization()) {
      metaData.clear();
    }
    if (status.isNewTransaction()) {
      doCleanupAfterCompletion(metaData, status.getTransaction());
    }
    if (status.getSuspendedResources() != null) {
//			log.debug("Resuming suspended transaction after completion of inner transaction");
      Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
      resume(metaData, transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
    }
  }

  // ---------------------------------------------------------------------
  // Template methods to be implemented in subclasses
  // ---------------------------------------------------------------------

  /**
   * Return a transaction object for the current transaction state.
   * <p>
   * The returned object will usually be specific to the concrete transaction
   * manager implementation, carrying corresponding transaction state in a
   * modifiable fashion. This object will be passed into the other template
   * methods (e.g. doBegin and doCommit), either directly or as part of a
   * DefaultTransactionStatus instance.
   * <p>
   * The returned object should contain information about any existing
   * transaction, that is, a transaction that has already started before the
   * current {@code getTransaction} call on the transaction manager. Consequently,
   * a {@code doGetTransaction} implementation will usually look for an existing
   * transaction and store corresponding state in the returned transaction object.
   *
   * @return the current transaction object
   * @throws CannotCreateTransactionException
   *             if transaction support is not available
   * @throws TransactionException
   *             in case of lookup or system errors
   * @see #doBegin
   * @see #doCommit
   * @see #doRollback
   * @see DefaultTransactionStatus#getTransaction
   */
  protected abstract Object doGetTransaction() throws TransactionException;

  /**
   * Check if the given transaction object indicates an existing transaction (that
   * is, a transaction which has already started).
   * <p>
   * The result will be evaluated according to the specified propagation behavior
   * for the new transaction. An existing transaction might get suspended (in case
   * of PROPAGATION_REQUIRES_NEW), or the new transaction might participate in the
   * existing one (in case of PROPAGATION_REQUIRED).
   * <p>
   * The default implementation returns {@code false}, assuming that participating
   * in existing transactions is generally not supported. Subclasses are of course
   * encouraged to provide such support.
   *
   * @param transaction
   *            transaction object returned by doGetTransaction
   * @return if there is an existing transaction
   * @throws TransactionException
   *             in case of system errors
   * @see #doGetTransaction
   */
  protected boolean isExistingTransaction(Object transaction) throws TransactionException {
    return false;
  }

  /**
   * Return whether to use a savepoint for a nested transaction.
   * <p>
   * Default is {@code true}, which causes delegation to DefaultTransactionStatus
   * for creating and holding a savepoint. If the transaction object does not
   * implement the SavepointManager interface, a
   * NestedTransactionNotSupportedException will be thrown. Else, the
   * SavepointManager will be asked to create a new savepoint to demarcate the
   * start of the nested transaction.
   * <p>
   * Subclasses can override this to return {@code false}, causing a further call
   * to {@code doBegin} - within the context of an already existing transaction.
   * The {@code doBegin} implementation needs to handle this accordingly in such a
   * scenario. This is appropriate for JTA, for example.
   *
   * @see DefaultTransactionStatus#createAndHoldSavepoint
   * @see DefaultTransactionStatus#rollbackToHeldSavepoint
   * @see DefaultTransactionStatus#releaseHeldSavepoint
   * @see #doBegin
   */
  protected boolean useSavepointForNestedTransaction() {
    return true;
  }

  /**
   * Begin a new transaction with semantics according to the given transaction
   * definition. Does not have to care about applying the propagation behavior, as
   * this has already been handled by this abstract manager.
   * <p>
   * This method gets called when the transaction manager has decided to actually
   * start a new transaction. Either there wasn't any transaction before, or the
   * previous transaction has been suspended.
   * <p>
   * A special scenario is a nested transaction without savepoint: If
   * {@code useSavepointForNestedTransaction()} returns "false", this method will
   * be called to start a nested transaction when necessary. In such a context,
   * there will be an active transaction: The implementation of this method has to
   * detect this and start an appropriate nested transaction.
   *
   * @param transaction
   *            transaction object returned by {@code doGetTransaction}
   * @param definition
   *            TransactionDefinition instance, describing propagation behavior,
   *            isolation level, read-only flag, timeout, and transaction name
   * @throws TransactionException
   *             in case of creation or system errors
   */
  protected abstract void doBegin(final SynchronizationMetaData metaData,
                                  final Object transaction, final TransactionDefinition definition)
          throws TransactionException;

  /**
   * Suspend the resources of the current transaction. Transaction synchronization
   * will already have been suspended.
   * <p>
   * The default implementation throws a
   * TransactionSuspensionNotSupportedException, assuming that transaction
   * suspension is generally not supported.
   *
   * @param transaction
   *            transaction object returned by {@code doGetTransaction}
   * @return an object that holds suspended resources (will be kept unexamined for
   *         passing it into doResume)
   * @throws TransactionException
   *             if suspending is not supported by the transaction manager
   *             implementation
   * @throws TransactionException
   *             in case of system errors
   * @see #doResume
   */
  protected Object doSuspend(final SynchronizationMetaData metaData, final Object transaction) {
    throw new TransactionException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
  }

  /**
   * Resume the resources of the current transaction. Transaction synchronization
   * will be resumed afterwards.
   * <p>
   * The default implementation throws a
   * TransactionSuspensionNotSupportedException, assuming that transaction
   * suspension is generally not supported.
   *
   * @param transaction
   *            transaction object returned by {@code doGetTransaction}
   * @param suspendedResources
   *            the object that holds suspended resources, as returned by
   *            doSuspend
   * @throws TransactionException
   *             if resuming is not supported by the transaction manager
   *             implementation
   * @see #doSuspend
   */
  protected void doResume(final SynchronizationMetaData metaData,
                          final Object transaction,
                          final Object suspendedResources) throws TransactionException {

    throw new TransactionException("Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
  }

  /**
   * Return whether to call {@code doCommit} on a transaction that has been marked
   * as rollback-only in a global fashion.
   * <p>
   * Does not apply if an application locally sets the transaction to
   * rollback-only via the TransactionStatus, but only to the transaction itself
   * being marked as rollback-only by the transaction coordinator.
   * <p>
   * Default is "false": Local transaction strategies usually don't hold the
   * rollback-only marker in the transaction itself, therefore they can't handle
   * rollback-only transactions as part of transaction commit. Hence,
   * AbstractPlatformTransactionManager will trigger a rollback in that case,
   * throwing an UnexpectedRollbackException afterwards.
   * <p>
   * Override this to return "true" if the concrete transaction manager expects a
   * {@code doCommit} call even for a rollback-only transaction, allowing for
   * special handling there. This will, for example, be the case for JTA, where
   * {@code UserTransaction.commit} will check the read-only flag itself and throw
   * a corresponding RollbackException, which might include the specific reason
   * (such as a transaction timeout).
   * <p>
   * If this method returns "true" but the {@code doCommit} implementation does
   * not throw an exception, this transaction manager will throw an
   * UnexpectedRollbackException itself. This should not be the typical case; it
   * is mainly checked to cover misbehaving JTA providers that silently roll back
   * even when the rollback has not been requested by the calling code.
   *
   * @see #doCommit
   * @see DefaultTransactionStatus#isGlobalRollbackOnly()
   * @see DefaultTransactionStatus#isLocalRollbackOnly()
   * @see TransactionStatus#setRollbackOnly()
   * @see UnexpectedRollbackException
   * @see javax.transaction.UserTransaction#commit()
   * @see javax.transaction.RollbackException
   */
  protected boolean shouldCommitOnGlobalRollbackOnly() {
    return false;
  }

  /**
   * Make preparations for commit, to be performed before the {@code beforeCommit}
   * synchronization callbacks occur.
   * <p>
   * Note that exceptions will get propagated to the commit caller and cause a
   * rollback of the transaction.
   *
   * @param status
   *            the status representation of the transaction
   * @throws RuntimeException
   *             in case of errors; will be <b>propagated to the caller</b> (note:
   *             do not throw TransactionException subclasses here!)
   */
  protected void prepareForCommit(final SynchronizationMetaData metaData, DefaultTransactionStatus status) {}

  /**
   * Perform an actual commit of the given transaction.
   * <p>
   * An implementation does not need to check the "new transaction" flag or the
   * rollback-only flag; this will already have been handled before. Usually, a
   * straight commit will be performed on the transaction object contained in the
   * passed-in status.
   *
   * @param status
   *            the status representation of the transaction
   * @throws TransactionException
   *             in case of commit or system errors
   * @see DefaultTransactionStatus#getTransaction
   */
  protected abstract void doCommit(final SynchronizationMetaData metaData, DefaultTransactionStatus status) throws TransactionException;

  /**
   * Perform an actual rollback of the given transaction.
   * <p>
   * An implementation does not need to check the "new transaction" flag; this
   * will already have been handled before. Usually, a straight rollback will be
   * performed on the transaction object contained in the passed-in status.
   *
   * @param status
   *            the status representation of the transaction
   * @throws TransactionException
   *             in case of system errors
   * @see DefaultTransactionStatus#getTransaction
   */
  protected abstract void doRollback(final SynchronizationMetaData metaData,
                                     final DefaultTransactionStatus status) throws TransactionException;

  /**
   * Set the given transaction rollback-only. Only called on rollback if the
   * current transaction participates in an existing one.
   * <p>
   * The default implementation throws an IllegalStateException, assuming that
   * participating in existing transactions is generally not supported. Subclasses
   * are of course encouraged to provide such support.
   *
   * @param status
   *            the status representation of the transaction
   * @throws TransactionException
   *             in case of system errors
   */
  protected void doSetRollbackOnly(final SynchronizationMetaData metaData, DefaultTransactionStatus status) throws TransactionException {
    throw new IllegalStateException("Participating in existing transactions is not supported - when 'isExistingTransaction' "
                                            + "returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
  }

  /**
   * Register the given list of transaction synchronizations with the existing
   * transaction.
   * <p>
   * Invoked when the control of the transaction manager and thus all
   * transaction synchronizations end, without the transaction being
   * completed yet. This is for example the case when participating in an existing
   * JTA or EJB CMT transaction.
   * <p>
   * The default implementation simply invokes the {@code afterCompletion} methods
   * immediately, passing in "STATUS_UNKNOWN". This is the best we can do if
   * there's no chance to determine the actual outcome of the outer transaction.
   *
   * @param transaction
   *            transaction object returned by {@code doGetTransaction}
   * @param syncs
   *            List of TransactionSynchronization objects
   * @throws TransactionException
   *             in case of system errors
   * @see #invokeAfterCompletion(SynchronizationMetaData, List, int)
   * @see TransactionSynchronization#afterCompletion(SynchronizationMetaData, int)
   * @see TransactionSynchronization#STATUS_UNKNOWN
   */
  protected void registerAfterCompletionWithExistingTransaction(final SynchronizationMetaData metaData,
                                                                final Object transaction,
                                                                final List<TransactionSynchronization> syncs)
          throws TransactionException {
    invokeAfterCompletion(metaData, syncs, TransactionSynchronization.STATUS_UNKNOWN);
  }

  /**
   * Cleanup resources after transaction completion.
   * <p>
   * Called after {@code doCommit} and {@code doRollback} execution, on any
   * outcome. The default implementation does nothing.
   * <p>
   * Should not throw any exceptions but just issue warnings on errors.
   *
   * @param transaction
   *            transaction object returned by {@code doGetTransaction}
   */
  protected void doCleanupAfterCompletion(final SynchronizationMetaData metaData, Object transaction) {}

  // ---------------------------------------------------------------------
  // Serialization support
  // ---------------------------------------------------------------------

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();
  }

  /**
   * Holder for suspended resources. Used internally by {@code suspend} and
   * {@code resume}.
   */
  protected static class SuspendedResourcesHolder {

    private final Object suspendedResources;

    private List<TransactionSynchronization> suspendedSynchronizations;

    private String name;

    private boolean readOnly;

    private int isolationLevel;

    private boolean wasActive;

    private SuspendedResourcesHolder(Object suspendedResources) {
      this.suspendedResources = suspendedResources;
    }

    private SuspendedResourcesHolder(// @off
                                        String name,
                                        boolean readOnly,
                                        boolean wasActive,
                                        int isolationLevel,
                                        Object suspendedResources,
                                        List<TransactionSynchronization> suspendedSynchronizations) {

            this.suspendedResources = suspendedResources;
            this.suspendedSynchronizations = suspendedSynchronizations;
            this.name = name;
            this.readOnly = readOnly;
            this.isolationLevel = isolationLevel;
            this.wasActive = wasActive; //@on
    }
  }

}
