/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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