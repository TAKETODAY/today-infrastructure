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

package infra.transaction.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.transaction.support.AbstractPlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionPropertiesTests {

  @Test
  void customizeAppliesConfiguredTimeoutAndRollbackPolicy() {
    var properties = new TransactionProperties();
    properties.defaultTimeout = Duration.ofSeconds(42);
    properties.rollbackOnCommitFailure = true;
    var transactionManager = new TestTransactionManager();

    properties.customize(transactionManager);

    assertThat(transactionManager.getDefaultTimeout()).isEqualTo(42);
    assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
  }

  @Test
  void customizeLeavesUnsetManagerPropertiesUntouched() {
    var properties = new TransactionProperties();
    var transactionManager = new TestTransactionManager();
    transactionManager.setDefaultTimeout(17);
    transactionManager.setRollbackOnCommitFailure(true);

    properties.customize(transactionManager);

    assertThat(transactionManager.getDefaultTimeout()).isEqualTo(17);
    assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
  }

  private static class TestTransactionManager extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() {
      return null;
    }

    @Override
    protected void doBegin(Object transaction, infra.transaction.TransactionDefinition definition) {
    }

    @Override
    protected void doCommit(infra.transaction.support.DefaultTransactionStatus status) {
    }

    @Override
    protected void doRollback(infra.transaction.support.DefaultTransactionStatus status) {
    }

  }

}
