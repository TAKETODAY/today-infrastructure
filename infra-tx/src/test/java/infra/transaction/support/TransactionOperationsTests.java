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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import infra.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:30
 */
class TransactionOperationsTests {

  @Test
  void shouldExecuteTransactionCallbackAndReturnResult() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    String expectedResult = "result";

    String result = operations.execute(status -> expectedResult);

    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void shouldExecuteTransactionCallbackWithConfigAndReturnResult() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    String expectedResult = "result";
    TransactionDefinition config = TransactionDefinition.withDefaults();

    String result = operations.execute(status -> expectedResult, config);

    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void shouldExecuteWithoutResultUsingRunnable() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    AtomicBoolean executed = new AtomicBoolean(false);

    operations.executeWithoutResult(status -> executed.set(true));

    assertThat(executed.get()).isTrue();
  }

  @Test
  void shouldExecuteWithoutResultUsingRunnableAndConfig() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    AtomicBoolean executed = new AtomicBoolean(false);
    TransactionDefinition config = TransactionDefinition.forReadOnly();

    operations.executeWithoutResult(status -> executed.set(true), config);

    assertThat(executed.get()).isTrue();
  }

  @Test
  void shouldPropagateRuntimeExceptionFromCallback() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    RuntimeException expectedException = new RuntimeException("test exception");

    assertThatThrownBy(() -> operations.execute(status -> {
      throw expectedException;
    })).isSameAs(expectedException);
  }

  @Test
  void shouldPropagateRuntimeExceptionFromCallbackWithoutResult() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    RuntimeException expectedException = new RuntimeException("test exception");

    assertThatThrownBy(() -> operations.executeWithoutResult(status -> {
      throw expectedException;
    })).isSameAs(expectedException);
  }

  @Test
  void shouldHandleNullReturnValueFromCallback() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();

    Object result = operations.execute(status -> null);

    assertThat(result).isNull();
  }

  @Test
  void shouldExecuteCallbackWithTransactionStatusAccess() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    AtomicBoolean statusAccessed = new AtomicBoolean(false);

    operations.execute(status -> {
      statusAccessed.set(status != null);
      return "done";
    });

    assertThat(statusAccessed.get()).isTrue();
  }

  @Test
  void shouldExecuteCallbackWithoutResultWithTransactionStatusAccess() {
    TransactionOperations operations = TransactionOperations.withoutTransaction();
    AtomicBoolean statusAccessed = new AtomicBoolean(false);

    operations.executeWithoutResult(status -> statusAccessed.set(status != null));

    assertThat(statusAccessed.get()).isTrue();
  }

}