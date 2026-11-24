/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.transaction.jta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import infra.transaction.HeuristicCompletionException;
import infra.transaction.IllegalTransactionStateException;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;
import infra.transaction.support.AbstractPlatformTransactionManager;
import infra.transaction.support.DefaultTransactionDefinition;
import infra.transaction.support.DefaultTransactionStatus;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JtaTransactionManagerBehaviorTests {

  @Test
  void defersCompletionUntilTheExternalTransactionCompletes() throws Exception {
    UserTransaction userTransaction = mock();
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    TransactionSynchronizationRegistry registry = mock();
    var manager = new JtaTransactionManager(userTransaction);
    manager.setTransactionSynchronizationRegistry(registry);
    TransactionSynchronization synchronization = mock();

    var participation = manager.getTransaction(null);
    TransactionSynchronizationManager.registerSynchronization(synchronization);
    manager.commit(participation);

    var callback = ArgumentCaptor.forClass(Synchronization.class);
    verify(registry).registerInterposedSynchronization(callback.capture());
    verify(synchronization, never()).afterCompletion(anyInt());
    verify(userTransaction, never()).commit();
    callback.getValue().afterCompletion(Status.STATUS_COMMITTED);
    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = { Status.STATUS_NO_TRANSACTION, Status.STATUS_ROLLEDBACK })
  void reportsRollbackWhenExternalTransactionHasAlreadyEnded(int jtaStatus) throws Exception {
    UserTransaction userTransaction = mock();
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE, jtaStatus);
    var manager = new JtaTransactionManager(userTransaction);
    TransactionSynchronization synchronization = mock();

    var participation = manager.getTransaction(null);
    TransactionSynchronizationManager.registerSynchronization(synchronization);
    manager.commit(participation);

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertThat(participation.isCompleted()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void reportsUnknownCompletionWhenProviderCannotSupplyExternalTransaction(boolean configuredManager) throws Exception {
    UserTransaction userTransaction = mock();
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    var manager = new JtaTransactionManager(userTransaction);
    if (configuredManager) {
      manager.setTransactionManager(mock(TransactionManager.class));
    }
    TransactionSynchronization synchronization = mock();

    var participation = manager.getTransaction(null);
    TransactionSynchronizationManager.registerSynchronization(synchronization);
    manager.commit(participation);

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertThat(participation.isCompleted()).isTrue();
  }

  @Test
  void translatesProviderFailureWhenRegisteringWithExternalTransaction() throws Exception {
    UserTransaction userTransaction = mock();
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    TransactionManager provider = mock();
    var failure = new SystemException("cannot retrieve external transaction");
    given(provider.getTransaction()).willThrow(failure);
    var manager = new JtaTransactionManager(userTransaction, provider);

    var participation = manager.getTransaction(null);
    TransactionSynchronizationManager.registerSynchronization(mock(TransactionSynchronization.class));

    assertThatThrownBy(() -> manager.commit(participation))
            .isInstanceOf(TransactionSystemException.class).hasCause(failure);
    assertThat(participation.isCompleted()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  void translatesJtaResumeFailures() throws Exception {
    TransactionManager provider = mock();
    Transaction suspended = mock();
    var manager = new JtaTransactionManager(mock(UserTransaction.class), provider);
    var invalid = new InvalidTransactionException("invalid transaction");
    var illegalState = new IllegalStateException("unexpected provider state");
    var system = new SystemException("resume failed");
    willThrow(invalid, illegalState, system).given(provider).resume(suspended);

    assertThatThrownBy(() -> manager.doResume(null, suspended))
            .isInstanceOf(IllegalTransactionStateException.class).hasCause(invalid);
    assertThatThrownBy(() -> manager.doResume(null, suspended))
            .isInstanceOf(TransactionSystemException.class).hasCause(illegalState);
    assertThatThrownBy(() -> manager.doResume(null, suspended))
            .isInstanceOf(TransactionSystemException.class).hasCause(system);
  }

  @Test
  void commitsAnActiveJtaTransaction() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    var manager = manager(userTransaction);

    var status = manager.getTransaction(new DefaultTransactionDefinition());
    manager.commit(status);

    var order = inOrder(userTransaction);
    order.verify(userTransaction).begin();
    order.verify(userTransaction).commit();
    order.verifyNoMoreInteractions();
  }

  @Test
  void mapsJtaCommitFailuresToFrameworkExceptions() throws Exception {
    assertCommitFailure(new RollbackException("rolled back"), UnexpectedRollbackException.class);
    assertCommitFailure(new HeuristicMixedException("mixed"), HeuristicCompletionException.class);
    assertCommitFailure(new HeuristicRollbackException("heuristic rollback"), HeuristicCompletionException.class);
    assertCommitFailure(new SystemException("system failure"), TransactionSystemException.class);
  }

  @Test
  void rejectsEnforcedReadOnlyWhenJtaApiDoesNotSupportIt() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    var manager = manager(userTransaction);
    manager.setEnforceReadOnly(true);
    var definition = new DefaultTransactionDefinition();
    definition.setReadOnly(true);

    assertThatThrownBy(() -> manager.getTransaction(definition))
            .isInstanceOf(infra.transaction.NestedTransactionNotSupportedException.class)
            .hasCauseInstanceOf(jakarta.transaction.NotSupportedException.class);
  }

  @Test
  void handlesAlreadyRolledBackJtaTransactions() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ROLLEDBACK);
    var manager = manager(userTransaction);

    var status = manager.getTransaction(new DefaultTransactionDefinition());
    assertThatThrownBy(() -> manager.commit(status))
            .isInstanceOf(UnexpectedRollbackException.class)
            .hasMessageContaining("already rolled back");
  }

  @Test
  void mapsRollbackFailuresToTransactionSystemException() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    willThrow(new SystemException("rollback failure")).given(userTransaction).rollback();
    var manager = manager(userTransaction);

    var status = manager.getTransaction(new DefaultTransactionDefinition());
    assertThatThrownBy(() -> manager.rollback(status))
            .isInstanceOf(TransactionSystemException.class)
            .hasMessageContaining("JTA failure on rollback");
  }

  @Test
  void commitsReadOnlyTransactionByRollingItBackWhenEnforced() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    JtaTransactionManager manager = manager(userTransaction);
    manager.setEnforceReadOnly(true);

    manager.doCommit(status(userTransaction, true));

    verify(userTransaction).rollback();
    verify(userTransaction, never()).commit();
  }

  @Test
  void mapsAlreadyCompletedCommitToUnexpectedRollback() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
    JtaTransactionManager manager = manager(userTransaction);

    assertThatThrownBy(() -> manager.doCommit(status(userTransaction, false)))
            .isInstanceOf(UnexpectedRollbackException.class)
            .hasMessageContaining("already completed");
  }

  @Test
  void ignoresIllegalStateWhenRollingBackAlreadyRolledBackTransaction() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ROLLEDBACK);
    willThrow(new IllegalStateException("already rolled back")).given(userTransaction).rollback();
    JtaTransactionManager manager = manager(userTransaction);

    manager.doRollback(status(userTransaction, false));

    verify(userTransaction).rollback();
  }

  @Test
  void mapsIllegalStateOnRollbackToTransactionSystemException() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new IllegalStateException("unexpected state")).given(userTransaction).rollback();
    JtaTransactionManager manager = manager(userTransaction);

    assertThatThrownBy(() -> manager.doRollback(status(userTransaction, false)))
            .isInstanceOf(TransactionSystemException.class)
            .hasMessageContaining("Unexpected internal transaction state");
  }

  @Test
  void skipsRollbackOnlyForCompletedJtaTransaction() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ROLLEDBACK);
    JtaTransactionManager manager = manager(userTransaction);

    manager.doSetRollbackOnly(status(userTransaction, false));
    manager.doSetRollbackOnly(status(userTransaction, false));

    verify(userTransaction, never()).setRollbackOnly();
  }

  @Test
  void resetsJtaTimeoutAfterTransactionCompletion() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    JtaTransactionManager manager = manager(userTransaction);
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setTimeout(5);

    manager.commit(manager.getTransaction(definition));

    org.mockito.Mockito.verify(userTransaction).setTransactionTimeout(5);
    org.mockito.Mockito.verify(userTransaction).setTransactionTimeout(0);
  }

  @Test
  void allowsCustomIsolationLevelsOnlyWhenConfigured() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    var manager = manager(userTransaction);
    var definition = new DefaultTransactionDefinition();
    definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

    assertThatThrownBy(() -> manager.getTransaction(definition))
            .isInstanceOf(infra.transaction.InvalidIsolationLevelException.class);

    manager.setAllowCustomIsolationLevels(true);
    manager.commit(manager.getTransaction(definition));
  }

  @Test
  void transactionFactoryConfiguresTimeoutAndStartsTransaction() throws Exception {
    var transactionManager = mock(jakarta.transaction.TransactionManager.class);
    var transaction = mock(jakarta.transaction.Transaction.class);
    given(transactionManager.getTransaction()).willReturn(transaction);
    var manager = new JtaTransactionManager(transactionManager);

    assertThat(manager.createTransaction("test", 12)).isNotNull();

    var order = inOrder(transactionManager);
    order.verify(transactionManager).setTransactionTimeout(12);
    order.verify(transactionManager).begin();
  }

  @Test
  void transactionFactoryAppliesZeroTimeoutButNotDefaultTimeout() throws Exception {
    var transactionManager = mock(jakarta.transaction.TransactionManager.class);
    var manager = new JtaTransactionManager(transactionManager);

    manager.createTransaction("zero", 0);
    manager.createTransaction("default", TransactionDefinition.TIMEOUT_DEFAULT);

    verify(transactionManager).setTransactionTimeout(0);
    verify(transactionManager, never()).setTransactionTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
    verify(transactionManager, org.mockito.Mockito.times(2)).begin();
  }

  @Test
  void mapsIllegalStateWhenCleaningUpAlreadyRolledBackJtaTransactionDuringCommit() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ROLLEDBACK);
    willThrow(new IllegalStateException("already rolled back")).given(userTransaction).rollback();
    JtaTransactionManager manager = manager(userTransaction);

    assertThatThrownBy(() -> manager.doCommit(status(userTransaction, false)))
            .isInstanceOf(UnexpectedRollbackException.class)
            .hasMessageContaining("already rolled back");
    verify(userTransaction).rollback();
  }

  @Test
  void mapsRollbackOnlyFailuresFromJtaProvider() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    willThrow(new IllegalStateException("illegal state"), new SystemException("system failure"))
            .given(userTransaction).setRollbackOnly();
    JtaTransactionManager manager = manager(userTransaction);

    assertThatThrownBy(() -> manager.doSetRollbackOnly(status(userTransaction, false)))
            .isInstanceOf(TransactionSystemException.class)
            .hasMessageContaining("Unexpected internal transaction state");
    assertThatThrownBy(() -> manager.doSetRollbackOnly(status(userTransaction, false)))
            .isInstanceOf(TransactionSystemException.class)
            .hasMessageContaining("JTA failure on setRollbackOnly");
  }

  @Test
  void doesNotInvokeRollbackWhenJtaTransactionHasAlreadyCompleted() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
    JtaTransactionManager manager = manager(userTransaction);

    manager.doRollback(status(userTransaction, false));

    verify(userTransaction, never()).rollback();
  }

  @Test
  void doesNotResetJtaTimeoutWhenConfiguredTimeoutIsZero() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    JtaTransactionManager manager = manager(userTransaction);
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setTimeout(0);

    manager.commit(manager.getTransaction(definition));

    verify(userTransaction).setTransactionTimeout(0);
    verify(userTransaction, org.mockito.Mockito.times(1)).setTransactionTimeout(0);
  }

  @Test
  void rejectsSuspensionWithoutJtaTransactionManager() throws Exception {
    JtaTransactionManager manager = manager(mock(UserTransaction.class));
    JtaTransactionObject transaction = new JtaTransactionObject(mock(UserTransaction.class));

    assertThatThrownBy(() -> manager.doJtaSuspend(transaction))
            .isInstanceOf(infra.transaction.TransactionSuspensionNotSupportedException.class);
    assertThatThrownBy(() -> manager.doJtaResume(transaction, mock(Transaction.class)))
            .isInstanceOf(infra.transaction.TransactionSuspensionNotSupportedException.class);
  }

  @Test
  void immediatelyRollsBackCallbacksWhenJtaTransactionIsMarkedRollbackOnly() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    TransactionManager transactionManager = mock();
    Transaction transaction = mock();
    given(transactionManager.getTransaction()).willReturn(transaction);
    willThrow(new RollbackException("marked rollback")).given(transaction).registerSynchronization(any());
    var manager = new JtaTransactionManager(userTransaction, transactionManager);
    TransactionSynchronization synchronization = mock();

    manager.registerAfterCompletionWithExistingTransaction(
            new JtaTransactionObject(userTransaction), List.of(synchronization));

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  void immediatelyMarksCallbacksUnknownWhenExternalRegistrationFailsUnexpectedly() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    TransactionManager transactionManager = mock();
    Transaction transaction = mock();
    given(transactionManager.getTransaction()).willReturn(transaction);
    willThrow(new IllegalStateException("provider state"))
            .given(transaction).registerSynchronization(any());
    var manager = new JtaTransactionManager(userTransaction, transactionManager);
    TransactionSynchronization synchronization = mock();

    manager.registerAfterCompletionWithExistingTransaction(
            new JtaTransactionObject(userTransaction), List.of(synchronization));

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  void transactionFactoryRequiresTransactionManager() {
    var manager = new JtaTransactionManager();

    assertThatThrownBy(() -> manager.createTransaction("missing", 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No JTA TransactionManager available");
  }

  @Test
  void doesNotSupportResourceAdapterManagedTransactions() {
    assertThat(new JtaTransactionManager().supportsResourceAdapterManagedTransactions()).isFalse();
  }

  @Test
  void ignoresFailureWhenResettingJtaTimeoutAfterCompletion() throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    willThrow(new SystemException("reset failed")).given(userTransaction).setTransactionTimeout(0);
    JtaTransactionManager manager = manager(userTransaction);
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setTimeout(5);

    manager.commit(manager.getTransaction(definition));

    verify(userTransaction).setTransactionTimeout(5);
    verify(userTransaction).setTransactionTimeout(0);
  }

  @Test
  void rejectsProgrammaticUseWithoutAUserTransaction() {
    var manager = new JtaTransactionManager();

    assertThatThrownBy(() -> manager.getTransaction(null))
            .isInstanceOf(infra.transaction.CannotCreateTransactionException.class)
            .hasMessageContaining("No JTA UserTransaction available");
  }

  private void assertCommitFailure(Exception failure, Class<? extends Throwable> expectedType) throws Exception {
    UserTransaction userTransaction = activeUserTransaction();
    willThrow(failure).given(userTransaction).commit();
    var manager = manager(userTransaction);

    var status = manager.getTransaction(new DefaultTransactionDefinition());
    assertThatThrownBy(() -> manager.commit(status)).isInstanceOf(expectedType);
  }

  private UserTransaction activeUserTransaction() throws SystemException {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
    return userTransaction;
  }

  private JtaTransactionManager manager(UserTransaction userTransaction) {
    var manager = new JtaTransactionManager(userTransaction);
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER);
    return manager;
  }

  private DefaultTransactionStatus status(UserTransaction userTransaction, boolean readOnly) {
    return new DefaultTransactionStatus("test", new JtaTransactionObject(userTransaction),
            true, false, false, readOnly, false, null);
  }

}
