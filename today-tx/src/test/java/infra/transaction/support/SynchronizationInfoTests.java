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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 21:34
 */
class SynchronizationInfoTests {

  @Test
  void clearRemovesAllState() {
    SynchronizationInfo info = new SynchronizationInfo();

    info.initSynchronization();
    info.setActualTransactionActive(true);
    info.setCurrentTransactionIsolationLevel(1);
    info.setCurrentTransactionName("test");
    info.setCurrentTransactionReadOnly(true);
    info.bindResource("key", "value");

    info.clear();

    assertThat(info.isSynchronizationActive()).isFalse();
    assertThat(info.isActualTransactionActive()).isFalse();
    assertThat(info.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(info.getCurrentTransactionName()).isNull();
    assertThat(info.isCurrentTransactionReadOnly()).isFalse();
    assertThat(info.getResourceMap()).isNotEmpty();
  }

  @Test
  void resourceMapOperations() {
    SynchronizationInfo info = new SynchronizationInfo();

    info.bindResource("key1", "value1");
    info.bindResource("key2", "value2");

    assertThat(info.hasResource("key1")).isTrue();
    assertThat((String) info.getResource("key1")).isEqualTo("value1");
    assertThat(info.getResourceMap()).hasSize(2);

    Object removed = info.unbindResource("key1");
    assertThat(removed).isEqualTo("value1");
    assertThat(info.hasResource("key1")).isFalse();

    info.unbindResourceIfPossible("key2");
    assertThat(info.getResourceMap()).isEmpty();
  }

  @Test
  void bindResourceWithVoidResourceHolder() {
    SynchronizationInfo info = new SynchronizationInfo();
    ResourceHolder holder = new ResourceHolderSupport() { };
    holder.unbound();

    info.bindResource("key", holder);
    assertThat(info.hasResource("key")).isFalse();
  }

  @Test
  void synchronizationInitializationFailsWhenAlreadyActive() {
    SynchronizationInfo info = new SynchronizationInfo();
    info.initSynchronization();

    assertThatThrownBy(() -> info.initSynchronization())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot activate transaction synchronization - already active");
  }

  @Test
  void clearSynchronizationFailsWhenNotActive() {
    SynchronizationInfo info = new SynchronizationInfo();

    assertThatThrownBy(() -> info.clearSynchronization())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot deactivate transaction synchronization - not active");
  }

  @Test
  void registerSynchronizationFailsWhenNotActive() {
    SynchronizationInfo info = new SynchronizationInfo();
    TransactionSynchronization sync = new TransactionSynchronization() {};

    assertThatThrownBy(() -> info.registerSynchronization(sync))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Transaction synchronization is not active");
  }

  @Test
  void getSynchronizationsFailsWhenNotActive() {
    SynchronizationInfo info = new SynchronizationInfo();

    assertThatThrownBy(() -> info.getSynchronizations())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Transaction synchronization is not active");
  }

  @Test
  void bindResourceFailsWhenValueIsNull() {
    SynchronizationInfo info = new SynchronizationInfo();

    assertThatThrownBy(() -> info.bindResource("key", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Value is required");
  }

  @Test
  void bindResourceFailsWhenKeyAlreadyBound() {
    SynchronizationInfo info = new SynchronizationInfo();
    info.bindResource("key", "value1");

    assertThatThrownBy(() -> info.bindResource("key", "value2"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Already value [value1] for key [key] bound to thread");
  }

  @Test
  void unbindResourceFailsWhenKeyNotBound() {
    SynchronizationInfo info = new SynchronizationInfo();

    assertThatThrownBy(() -> info.unbindResource("key"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No value for key [key] bound to thread");
  }

  @Test
  void transactionAttributesAreInitiallyUnset() {
    SynchronizationInfo info = new SynchronizationInfo();

    assertThat(info.getCurrentTransactionName()).isNull();
    assertThat(info.isCurrentTransactionReadOnly()).isFalse();
    assertThat(info.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(info.isActualTransactionActive()).isFalse();
  }

  @Test
  void getResourceMapReturnsUnmodifiableMap() {
    SynchronizationInfo info = new SynchronizationInfo();
    info.bindResource("key", "value");

    Map<Object, Object> map = info.getResourceMap();
    assertThatThrownBy(() -> map.put("key2", "value2"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void triggerBeforeCompletionContinuesOnException() {
    SynchronizationInfo info = new SynchronizationInfo();
    info.initSynchronization();

    AtomicBoolean secondSyncCalled = new AtomicBoolean();

    TransactionSynchronization sync1 = new TransactionSynchronization() {
      @Override
      public void beforeCompletion() {
        throw new RuntimeException("Test Exception");
      }
    };

    TransactionSynchronization sync2 = new TransactionSynchronization() {
      @Override
      public void beforeCompletion() {
        secondSyncCalled.set(true);
      }
    };

    info.registerSynchronization(sync1);
    info.registerSynchronization(sync2);

    info.triggerBeforeCompletion();

    assertThat(secondSyncCalled).isTrue();
  }

  @Test
  void synchronizationsAreOrderedByPriority() {
    SynchronizationInfo info = new SynchronizationInfo();
    info.initSynchronization();

    List<Integer> executionOrder = new ArrayList<>();

    TransactionSynchronization sync1 = new TransactionSynchronization() {

      @Override
      public void flush() {
        executionOrder.add(2);
      }
    };

    TransactionSynchronization sync2 = new TransactionSynchronization() {
      @Override
      public int getOrder() { return 1; }

      @Override
      public void flush() {
        executionOrder.add(1);
      }
    };

    info.registerSynchronization(sync1);
    info.registerSynchronization(sync2);

    info.triggerFlush();

    assertThat(executionOrder).containsExactly(1, 2);
  }

}