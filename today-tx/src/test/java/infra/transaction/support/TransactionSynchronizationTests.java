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