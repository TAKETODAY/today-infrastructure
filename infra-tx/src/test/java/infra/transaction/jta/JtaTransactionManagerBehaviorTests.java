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

import infra.transaction.HeuristicCompletionException;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;
import infra.transaction.support.AbstractPlatformTransactionManager;
import infra.transaction.support.DefaultTransactionDefinition;
import infra.transaction.support.DefaultTransactionStatus;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class JtaTransactionManagerBehaviorTests {

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

}
