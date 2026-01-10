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

import java.util.List;

import infra.transaction.support.TransactionSynchronization;
import jakarta.transaction.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:24
 */
class JtaAfterCompletionSynchronizationTests {

  @Test
  void constructorInitializesSynchronizations() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThat(synchronization).isNotNull();
  }

  @Test
  void beforeCompletionDoesNotThrowException() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThatCode(synchronization::beforeCompletion).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithCommittedStatus() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThatCode(() -> synchronization.afterCompletion(Status.STATUS_COMMITTED)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithRolledBackStatus() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThatCode(() -> synchronization.afterCompletion(Status.STATUS_ROLLEDBACK)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithUnknownStatus() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThatCode(() -> synchronization.afterCompletion(Status.STATUS_UNKNOWN)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithOtherStatus() {
    List<TransactionSynchronization> synchronizations = List.of();
    JtaAfterCompletionSynchronization synchronization = new JtaAfterCompletionSynchronization(synchronizations);

    assertThatCode(() -> synchronization.afterCompletion(Status.STATUS_NO_TRANSACTION)).doesNotThrowAnyException();
  }

}