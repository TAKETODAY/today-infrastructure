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

import infra.transaction.TransactionSystemException;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JtaTransactionObjectTests {

  @Test
  void reportsRollbackOnlyForMarkedAndRolledBackStatuses() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    JtaTransactionObject transactionObject = new JtaTransactionObject(userTransaction);

    given(userTransaction.getStatus()).willReturn(Status.STATUS_MARKED_ROLLBACK, Status.STATUS_ROLLEDBACK,
            Status.STATUS_ACTIVE);

    assertThat(transactionObject.isRollbackOnly()).isTrue();
    assertThat(transactionObject.isRollbackOnly()).isTrue();
    assertThat(transactionObject.isRollbackOnly()).isFalse();
  }

  @Test
  void mapsStatusFailureToTransactionSystemException() throws Exception {
    UserTransaction userTransaction = mock(UserTransaction.class);
    given(userTransaction.getStatus()).willThrow(new SystemException("status failure"));

    assertThatThrownBy(() -> new JtaTransactionObject(userTransaction).isRollbackOnly())
            .isInstanceOf(TransactionSystemException.class)
            .hasMessage("JTA failure on getStatus");
  }

  @Test
  void flushTriggersRegisteredSynchronizations() {
    UserTransaction userTransaction = mock(UserTransaction.class);
    JtaTransactionObject transactionObject = new JtaTransactionObject(userTransaction);
    TransactionSynchronization synchronization = mock(TransactionSynchronization.class);

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(synchronization);
      transactionObject.flush();
      verify(synchronization).flush();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

}
