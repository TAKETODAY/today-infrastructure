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

package infra.transaction;

import org.junit.jupiter.api.Test;

import infra.transaction.annotation.Isolation;
import infra.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

  @Test
  void shouldCopyAndDescribeTransactionDefinition() {
    var original = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    original.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    original.setTimeout(30);
    original.setReadOnly(true);
    original.setName("ignored by description");

    var copy = new DefaultTransactionDefinition(original);
    assertThat(copy).isEqualTo(original);
    assertThat(copy.hashCode()).isEqualTo(original.hashCode());
    assertThat(copy.toString()).isEqualTo("PROPAGATION_REQUIRES_NEW,ISOLATION_SERIALIZABLE,timeout_30,readOnly");
    assertThat(copy).isNotEqualTo(null);
    assertThat(copy).isNotEqualTo("definition");
  }

  @Test
  void shouldRenderAllSupportedPropagationAndIsolationNames() {
    var definition = new DefaultTransactionDefinition();
    int[] propagation = {
            TransactionDefinition.PROPAGATION_REQUIRED,
            TransactionDefinition.PROPAGATION_SUPPORTS,
            TransactionDefinition.PROPAGATION_MANDATORY,
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            TransactionDefinition.PROPAGATION_NOT_SUPPORTED,
            TransactionDefinition.PROPAGATION_NEVER,
            TransactionDefinition.PROPAGATION_NESTED
    };
    for (int value : propagation) {
      definition.setPropagationBehavior(value);
      assertThat(definition.toString()).startsWith("PROPAGATION_");
    }
    int[] isolation = {
            TransactionDefinition.ISOLATION_DEFAULT,
            TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
            TransactionDefinition.ISOLATION_READ_COMMITTED,
            TransactionDefinition.ISOLATION_REPEATABLE_READ,
            TransactionDefinition.ISOLATION_SERIALIZABLE
    };
    for (int value : isolation) {
      definition.setIsolationLevel(value);
      assertThat(definition.toString()).contains("ISOLATION_");
    }
  }

  @Test
  void rejectsInvalidTimeoutAndRendersInvalidDefinitionValues() {
    var definition = new DefaultTransactionDefinition();
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> definition.setTimeout(-2));
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new DefaultTransactionDefinition(99).toString())
            .withMessageContaining("Unsupported propagation behavior: 99");
  }

}
