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