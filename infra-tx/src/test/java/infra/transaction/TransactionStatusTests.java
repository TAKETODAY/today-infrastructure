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