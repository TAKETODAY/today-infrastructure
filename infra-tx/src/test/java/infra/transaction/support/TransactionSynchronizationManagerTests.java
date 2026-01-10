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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:43
 */
@SuppressWarnings("cast")
class TransactionSynchronizationManagerTests {

  @Test
  void shouldBindAndRetrieveResource() {
    String key = "testKey";
    Object resource = new Object();

    TransactionSynchronizationManager.bindResource(key, resource);
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();
    assertThat((Object) TransactionSynchronizationManager.getResource(key)).isSameAs(resource);

    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldReturnResourceMap() {
    String key1 = "testKey1";
    String key2 = "testKey2";
    Object resource1 = new Object();
    Object resource2 = new Object();

    TransactionSynchronizationManager.bindResource(key1, resource1);
    TransactionSynchronizationManager.bindResource(key2, resource2);

    Map<Object, Object> resourceMap = TransactionSynchronizationManager.getResourceMap();
    assertThat(resourceMap).containsEntry(key1, resource1);
    assertThat(resourceMap).containsEntry(key2, resource2);

    TransactionSynchronizationManager.unbindResource(key1);
    TransactionSynchronizationManager.unbindResource(key2);
  }

  @Test
  void shouldThrowExceptionWhenBindingResourceToExistingKey() {
    String key = "testKey";
    Object resource1 = new Object();
    Object resource2 = new Object();

    TransactionSynchronizationManager.bindResource(key, resource1);

    assertThatThrownBy(() -> TransactionSynchronizationManager.bindResource(key, resource2))
            .isInstanceOf(IllegalStateException.class);

    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldUnbindResourceAndReturnPreviousValue() {
    String key = "testKey";
    Object resource = new Object();

    TransactionSynchronizationManager.bindResource(key, resource);
    Object unboundResource = TransactionSynchronizationManager.unbindResource(key);

    assertThat(unboundResource).isSameAs(resource);
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();
  }

  @Test
  void shouldThrowExceptionWhenUnbindingNonExistentResource() {
    String key = "nonExistentKey";

    assertThatThrownBy(() -> TransactionSynchronizationManager.unbindResource(key))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldUnbindResourceIfPossibleReturnsNullForNonExistentResource() {
    String key = "nonExistentKey";

    Object result = TransactionSynchronizationManager.unbindResourceIfPossible(key);
    assertThat(result).isNull();
  }

  @Test
  void shouldInitializeAndClearSynchronization() {
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    TransactionSynchronizationManager.initSynchronization();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();

    TransactionSynchronizationManager.clearSynchronization();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  void shouldRegisterAndRetrieveSynchronizations() {
    TransactionSynchronization sync1 = new TestTransactionSynchronization();
    TransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
      assertThat(synchronizations).hasSize(2);
      assertThat(synchronizations).containsExactly(sync1, sync2);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldThrowExceptionWhenRegisteringSynchronizationWithoutInitialization() {
    TransactionSynchronization sync = new TestTransactionSynchronization();

    assertThatThrownBy(() -> TransactionSynchronizationManager.registerSynchronization(sync))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldSetAndGetTransactionName() {
    String transactionName = "testTransaction";

    TransactionSynchronizationManager.setCurrentTransactionName(transactionName);
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo(transactionName);

    TransactionSynchronizationManager.setCurrentTransactionName(null);
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
  }

  @Test
  void shouldSetAndGetTransactionReadOnlyFlag() {
    TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();

    TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
  }

  @Test
  void shouldSetAndGetTransactionIsolationLevel() {
    Integer isolationLevel = 2; // ISOLATION_READ_COMMITTED

    TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(isolationLevel);
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isEqualTo(isolationLevel);

    TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
  }

  @Test
  void shouldSetAndGetActualTransactionActiveFlag() {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

    TransactionSynchronizationManager.setActualTransactionActive(false);
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void shouldClearEntireTransactionState() {
    // Set up various transaction states
    String key = "testKey";
    Object resource = new Object();
    TransactionSynchronizationManager.bindResource(key, resource);

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.registerSynchronization(new TestTransactionSynchronization());

    TransactionSynchronizationManager.setCurrentTransactionName("testTransaction");
    TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
    TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(2);
    TransactionSynchronizationManager.setActualTransactionActive(true);

    // Verify states are set
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNotNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNotNull();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

    // Clear and verify all states are reset
    TransactionSynchronizationManager.clear();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void shouldBindSynchronizedResourceWhenSynchronizationActive() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      String key = "testKey";
      Object resource = new Object();

      TransactionSynchronizationManager.bindSynchronizedResource(key, resource);
      assertThat((Object) TransactionSynchronizationManager.getResource(key)).isSameAs(resource);

      TransactionSynchronizationManager.unbindResource(key);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldThrowExceptionWhenBindingSynchronizedResourceWithoutActiveSynchronization() {
    String key = "testKey";
    Object resource = new Object();

    assertThatThrownBy(() -> TransactionSynchronizationManager.bindSynchronizedResource(key, resource))
            .isInstanceOf(IllegalStateException.class);
  }

  static class TestTransactionSynchronization implements TransactionSynchronization {
    @Override
    public void suspend() { }

    @Override
    public void resume() { }

    @Override
    public void flush() { }

    @Override
    public void beforeCommit(boolean readOnly) { }

    @Override
    public void beforeCompletion() { }

    @Override
    public void afterCommit() { }

    @Override
    public void afterCompletion(int status) { }

    @Override
    public void savepoint(Object savepoint) { }

    @Override
    public void savepointRollback(Object savepoint) { }
  }

}