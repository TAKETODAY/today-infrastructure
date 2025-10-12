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