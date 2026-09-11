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

package infra.persistence.event;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.jdbc.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DefaultEntityEventRegistryTests {

  private DefaultEntityEventRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new DefaultEntityEventRegistry();
  }

  @Test
  void addListenerRejectsNull() {
    assertThatThrownBy(() -> registry.addListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Listener is required");
  }

  @Test
  void addListenerRejectsUnsupportedListener() {
    assertThatThrownBy(() -> registry.addListener(new Listener() {
    }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported listener type");
  }

  @Test
  void listenersAreStoredByContract() {
    EntityEventListener<UserModel> entityListener = new UserEventListening();
    BatchPersistListener batchListener = (execution, implicitExecution, exception) -> {
    };

    registry.addListener(entityListener);
    registry.addListener(batchListener);

    assertThat(registry.listeners(EntityEventListener.class)).containsExactly(entityListener);
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(batchListener);
  }

  @Test
  void listenerImplementingMultipleContractsIsRegisteredUnderEach() {
    HybridListener hybrid = new HybridListener();

    registry.addListener(hybrid);

    assertThat(registry.listeners(EntityEventListener.class)).containsExactly(hybrid);
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(hybrid);
  }

  @Test
  void getListenersReturnsEmptyForUnknownContract() {
    assertThat(registry.listeners(EntityEventListener.class)).isEmpty();
    assertThat(registry.listeners(BatchPersistListener.class)).isEmpty();
  }

  @Test
  void getListenersReturnsEmptyAfterAllListenersOfContractAreRemoved() {
    EntityEventListener<UserModel> listener = new UserEventListening();
    registry.addListener(listener);

    registry.removeListener(listener);

    assertThat(registry.listeners(EntityEventListener.class)).isEmpty();
  }

  @Test
  void batchPersistListenersAreManagedByRegistry() {
    BatchPersistListener listener = (execution, implicitExecution, exception) -> {
    };

    registry.addListener(listener);
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(listener);

    registry.setListeners(List.of(listener));
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(listener);

    registry.setListeners(null);
    assertThat(registry.listeners(BatchPersistListener.class)).isEmpty();
  }

  @Test
  void entityEventListenersAreStoredByContract() {
    EntityEventListener<UserModel> listener = new EntityEventListener<>() {
    };

    registry.addListener(listener);
    assertThat(registry.listeners(EntityEventListener.class)).containsExactly(listener);

    registry.removeListener(listener);
    assertThat(registry.listeners(EntityEventListener.class)).isEmpty();
  }

  @Test
  void addListenersAcceptsNullCollection() {
    registry.addListeners(null);
    assertThat(registry.listeners(EntityEventListener.class)).isEmpty();
  }

  @Test
  void removeListenersRemovesMultipleListeners() {
    EntityEventListener<UserModel> first = new UserEventListening();
    EntityEventListener<UserModel> second = new UserEventListening();
    registry.addListeners(List.of(first, second));

    registry.removeListeners(List.of(first));

    assertThat(registry.listeners(EntityEventListener.class)).containsExactly(second);
  }

  @Test
  void clearRemovesAllListenersAcrossContracts() {
    registry.addListener(new UserEventListening());
    registry.addListener((BatchPersistListener) (execution, implicitExecution, exception) -> {
    });

    registry.clear();

    assertThat(registry.listeners(EntityEventListener.class)).isEmpty();
    assertThat(registry.listeners(BatchPersistListener.class)).isEmpty();
  }

  static class UserEventListening implements EntityEventListener<UserModel> {

  }

  static class ObjectEventListening implements EntityEventListener<Object> {

  }

  static class HybridListener implements EntityEventListener<UserModel>, BatchPersistListener {

    @Override
    public void afterPersist(EntityPersistEvent<UserModel> event) {
    }

    @Override
    public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
    }
  }

}
