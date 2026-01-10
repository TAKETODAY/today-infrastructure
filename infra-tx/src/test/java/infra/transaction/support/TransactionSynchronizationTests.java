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

import infra.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:26
 */
class TransactionSynchronizationTests {

  @Test
  void defaultMethods() {
    TransactionSynchronization synchronization = new TransactionSynchronization() { };

    assertThat(synchronization.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

    assertThatNoException().isThrownBy(() -> {
      synchronization.suspend();
      synchronization.resume();
      synchronization.flush();
      synchronization.beforeCommit(false);
      synchronization.beforeCompletion();
      synchronization.afterCommit();
      synchronization.afterCompletion(1);
      synchronization.savepoint(1);
      synchronization.savepointRollback(1);
    });

  }

}