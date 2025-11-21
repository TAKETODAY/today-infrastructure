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

package infra.transaction;

import org.junit.jupiter.api.Test;

import infra.transaction.annotation.Isolation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:24
 */
class TransactionDefinitionTests {

  @Test
  void shouldReturnDefaultPropagationBehavior() {
    TransactionDefinition definition = TransactionDefinition.withDefaults();
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void shouldReturnReadOnlyTransactionDefinition() {
    TransactionDefinition definition = TransactionDefinition.forReadOnly();
    assertThat(definition.isReadOnly()).isTrue();
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void shouldReturnTimeoutTransactionDefinition() {
    int timeout = 30;
    TransactionDefinition definition = TransactionDefinition.forTimeout(timeout);
    assertThat(definition.getTimeout()).isEqualTo(timeout);
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void shouldReturnIsolationLevelTransactionDefinitionUsingInt() {
    int isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE;
    TransactionDefinition definition = TransactionDefinition.forIsolationLevel(isolationLevel);
    assertThat(definition.getIsolationLevel()).isEqualTo(isolationLevel);
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void shouldReturnIsolationLevelTransactionDefinitionUsingEnum() {
    Isolation isolation = Isolation.READ_COMMITTED;
    TransactionDefinition definition = TransactionDefinition.forIsolationLevel(isolation);
    assertThat(definition.getIsolationLevel()).isEqualTo(isolation.value());
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void shouldReturnDefaultValuesFromInterfaceMethods() {
    TransactionDefinition definition = new TransactionDefinition() { };
    assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    assertThat(definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
    assertThat(definition.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
    assertThat(definition.isReadOnly()).isFalse();
    assertThat(definition.getName()).isNull();
  }

}