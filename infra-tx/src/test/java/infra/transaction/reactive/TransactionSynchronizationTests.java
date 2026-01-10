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