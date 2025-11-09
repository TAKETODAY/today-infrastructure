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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:54
 */
class TransactionSynchronizationTests {

  @Test
  void defaultSuspendReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.suspend()).isNotNull();
    assertThat(synchronization.suspend().block()).isNull();
  }

  @Test
  void defaultResumeReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.resume()).isNotNull();
    assertThat(synchronization.resume().block()).isNull();
  }

  @Test
  void defaultBeforeCommitReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.beforeCommit(false)).isNotNull();
    assertThat(synchronization.beforeCommit(false).block()).isNull();
  }

  @Test
  void defaultBeforeCompletionReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.beforeCompletion()).isNotNull();
    assertThat(synchronization.beforeCompletion().block()).isNull();
  }

  @Test
  void defaultAfterCommitReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.afterCommit()).isNotNull();
    assertThat(synchronization.afterCommit().block()).isNull();
  }

  @Test
  void defaultAfterCompletionReturnsEmptyMono() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };
    assertThat(synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)).isNotNull();
    assertThat(synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED).block()).isNull();
  }

  @Test
  void statusConstantsHaveCorrectValues() {
    assertThat(TransactionSynchronization.STATUS_COMMITTED).isEqualTo(0);
    assertThat(TransactionSynchronization.STATUS_ROLLED_BACK).isEqualTo(1);
    assertThat(TransactionSynchronization.STATUS_UNKNOWN).isEqualTo(2);
  }

}