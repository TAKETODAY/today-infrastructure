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

import infra.transaction.NestedTransactionNotSupportedException;
import infra.transaction.SavepointManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:26
 */
class DefaultTransactionStatusTests {

  @Test
  void shouldCreateDefaultTransactionStatusWithAllParameters() {
    Object transaction = new Object();
    String transactionName = "testTransaction";
    Object suspendedResources = new Object();

    DefaultTransactionStatus status = new DefaultTransactionStatus(
            transactionName, transaction, true, true, true, true, true, suspendedResources);

    assertThat(status.getTransaction()).isEqualTo(transaction);
    assertThat(status.getTransactionName()).isEqualTo(transactionName);
    assertThat(status.hasTransaction()).isTrue();
    assertThat(status.isNewTransaction()).isTrue();
    assertThat(status.isNewSynchronization()).isTrue();
    assertThat(status.isNested()).isTrue();
    assertThat(status.isReadOnly()).isTrue();
    assertThat(status.isDebug()).isTrue();
    assertThat(status.getSuspendedResources()).isEqualTo(suspendedResources);
  }

  @Test
  void shouldCreateTransactionStatusWithoutTransaction() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, null, false, false, false, false, false, null);

    assertThat(status.hasTransaction()).isFalse();
    assertThat(status.isNewTransaction()).isFalse();
    assertThat(status.getTransactionName()).isEmpty();
    assertThat(status.isNewSynchronization()).isFalse();
    assertThat(status.isNested()).isFalse();
    assertThat(status.isReadOnly()).isFalse();
    assertThat(status.isDebug()).isFalse();
    assertThat(status.getSuspendedResources()).isNull();
  }

  @Test
  void shouldThrowExceptionWhenGettingTransactionWithoutActiveTransaction() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, null, false, false, false, false, false, null);

    assertThatThrownBy(status::getTransaction)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No transaction active");
  }

  @Test
  void shouldReturnEmptyStringWhenTransactionNameIsNull() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, new Object(), true, false, false, false, false, null);

    assertThat(status.getTransactionName()).isEmpty();
  }

  @Test
  void shouldIdentifySavepointManagerCapability() {
    SavepointManager savepointManager = new MockSavepointManager();
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, savepointManager, true, false, true, false, false, null);

    assertThat(status.isTransactionSavepointManager()).isTrue();
  }

  @Test
  void shouldNotIdentifySavepointManagerCapability() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, new Object(), true, false, true, false, false, null);

    assertThat(status.isTransactionSavepointManager()).isFalse();
  }

  @Test
  void shouldGetSavepointManagerWhenSupported() {
    SavepointManager savepointManager = new MockSavepointManager();
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, savepointManager, true, false, true, false, false, null);

    assertThat(status.getSavepointManager()).isEqualTo(savepointManager);
  }

  @Test
  void shouldThrowExceptionWhenGettingSavepointManagerNotSupported() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, new Object(), true, false, true, false, false, null);

    assertThatThrownBy(status::getSavepointManager)
            .isInstanceOf(NestedTransactionNotSupportedException.class)
            .hasMessageContaining("does not support savepoints");
  }

  @Test
  void shouldIdentifyGlobalRollbackOnlyFromSmartTransactionObject() {
    SmartTransactionObject smartTransactionObject = new MockSmartTransactionObject(true);
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, smartTransactionObject, true, false, false, false, false, null);

    assertThat(status.isGlobalRollbackOnly()).isTrue();
  }

  @Test
  void shouldNotIdentifyGlobalRollbackOnlyFromNonSmartTransactionObject() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, new Object(), true, false, false, false, false, null);

    assertThat(status.isGlobalRollbackOnly()).isFalse();
  }

  @Test
  void shouldFlushSmartTransactionObject() {
    MockSmartTransactionObject smartTransactionObject = new MockSmartTransactionObject(false);
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, smartTransactionObject, true, false, false, false, false, null);

    status.flush();
    assertThat(smartTransactionObject.flushed).isTrue();
  }

  @Test
  void shouldNotFailWhenFlushingNonSmartTransactionObject() {
    DefaultTransactionStatus status = new DefaultTransactionStatus(
            null, new Object(), true, false, false, false, false, null);

    assertThatNoException().isThrownBy(status::flush);
  }

  static class MockSavepointManager implements SavepointManager {
    @Override
    public Object createSavepoint() {
      return null;
    }

    @Override
    public void rollbackToSavepoint(Object savepoint) {
    }

    @Override
    public void releaseSavepoint(Object savepoint) {
    }
  }

  static class MockSmartTransactionObject implements SmartTransactionObject {
    private final boolean rollbackOnly;
    boolean flushed = false;

    MockSmartTransactionObject(boolean rollbackOnly) {
      this.rollbackOnly = rollbackOnly;
    }

    @Override
    public boolean isRollbackOnly() {
      return rollbackOnly;
    }

    @Override
    public void flush() {
      flushed = true;
    }
  }

}