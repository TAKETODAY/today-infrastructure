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

package infra.transaction.reactive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:51
 */
class GenericReactiveTransactionTests {

  @Test
  void shouldCreateGenericReactiveTransactionWithAllParameters() {
    Object transaction = new Object();
    String transactionName = "testTransaction";
    Object suspendedResources = new Object();

    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            transactionName, transaction, true, true, true, true, true, suspendedResources);

    assertThat(reactiveTransaction.getTransaction()).isEqualTo(transaction);
    assertThat(reactiveTransaction.getTransactionName()).isEqualTo(transactionName);
    assertThat(reactiveTransaction.hasTransaction()).isTrue();
    assertThat(reactiveTransaction.isNewTransaction()).isTrue();
    assertThat(reactiveTransaction.isNewSynchronization()).isTrue();
    assertThat(reactiveTransaction.isNested()).isTrue();
    assertThat(reactiveTransaction.isReadOnly()).isTrue();
    assertThat(reactiveTransaction.isDebug()).isTrue();
    assertThat(reactiveTransaction.getSuspendedResources()).isEqualTo(suspendedResources);
    assertThat(reactiveTransaction.isRollbackOnly()).isFalse();
    assertThat(reactiveTransaction.isCompleted()).isFalse();
  }

  @Test
  void shouldCreateTransactionWithoutTransactionObject() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, null, false, false, false, false, false, null);

    assertThat(reactiveTransaction.hasTransaction()).isFalse();
    assertThat(reactiveTransaction.isNewTransaction()).isFalse();
    assertThat(reactiveTransaction.getTransactionName()).isEmpty();
    assertThat(reactiveTransaction.isNewSynchronization()).isFalse();
    assertThat(reactiveTransaction.isNested()).isFalse();
    assertThat(reactiveTransaction.isReadOnly()).isFalse();
    assertThat(reactiveTransaction.isDebug()).isFalse();
    assertThat(reactiveTransaction.getSuspendedResources()).isNull();
  }

  @Test
  void shouldThrowExceptionWhenGettingTransactionWithoutActiveTransaction() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, null, false, false, false, false, false, null);

    assertThatThrownBy(reactiveTransaction::getTransaction)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No transaction active");
  }

  @Test
  void shouldSetRollbackOnlyFlag() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, new Object(), true, false, false, false, false, null);

    assertThat(reactiveTransaction.isRollbackOnly()).isFalse();
    reactiveTransaction.setRollbackOnly();
    assertThat(reactiveTransaction.isRollbackOnly()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenSettingRollbackOnlyOnCompletedTransaction() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, new Object(), true, false, false, false, false, null);
    reactiveTransaction.setCompleted();

    assertThatThrownBy(reactiveTransaction::setRollbackOnly)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Transaction completed");
  }

  @Test
  void shouldMarkTransactionAsCompleted() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, new Object(), true, false, false, false, false, null);

    assertThat(reactiveTransaction.isCompleted()).isFalse();
    reactiveTransaction.setCompleted();
    assertThat(reactiveTransaction.isCompleted()).isTrue();
  }

  @Test
  void shouldReturnEmptyStringWhenTransactionNameIsNull() {
    GenericReactiveTransaction reactiveTransaction = new GenericReactiveTransaction(
            null, new Object(), true, false, false, false, false, null);

    assertThat(reactiveTransaction.getTransactionName()).isEmpty();
  }

}