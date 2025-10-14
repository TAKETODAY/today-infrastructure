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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 19:09
 */
class TransactionStatusTests {

  @Test
  void defaultFlushDoesNotThrowException() {
    TransactionStatus status = new TestTransactionStatus();
    assertThatNoException().isThrownBy(status::flush);
    assertThat(status.hasSavepoint()).isFalse();
    assertThat(status.isNested()).isFalse();
    assertThat(status.isReadOnly()).isFalse();
    assertThat(status.isCompleted()).isFalse();
    assertThat(status.isRollbackOnly()).isFalse();
    assertThat(status.hasTransaction()).isTrue();
    assertThat(status.isNewTransaction()).isTrue();

    assertThat(status.getTransactionName()).isEqualTo("");
    assertThatThrownBy(status::setRollbackOnly).isInstanceOf(UnsupportedOperationException.class).hasMessage("setRollbackOnly not supported");

  }

  static class TestTransactionStatus implements TransactionStatus {

    @Override
    public Object createSavepoint() throws TransactionException {
      return null;
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {

    }

    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {

    }
  }

}