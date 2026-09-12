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
import infra.persistence.EntityMetadata;
import infra.persistence.PropertyUpdateStrategy;

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
  void baseEntityEventListenerIsNotRegistrable() {
    // EntityEventListener is the common base contract and carries no callbacks, so
    // it is not a registrable contract on its own; a concrete contract is required.
    assertThatThrownBy(() -> registry.addListener(new EntityEventListener<>() {
    }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported listener type");
  }

  @Test
  void listenersAreStoredByContract() {
    PersistingEventListener<UserModel> entityListener = new UserEventListening();
    BatchPersistListener batchListener = (execution, implicitExecution, exception) -> {
    };

    registry.addListener(entityListener);
    registry.addListener(batchListener);

    assertThat(registry.listeners(PersistingEventListener.class)).containsExactly(entityListener);
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(batchListener);
  }

  @Test
  void listenerImplementingMultipleContractsIsRegisteredUnderEach() {
    HybridListener hybrid = new HybridListener();

    registry.addListener(hybrid);

    assertThat(registry.listeners(PersistingEventListener.class)).containsExactly(hybrid);
    assertThat(registry.listeners(BatchPersistListener.class)).containsExactly(hybrid);
  }

  @Test
  void getListenersReturnsEmptyForUnknownContract() {
    assertThat(registry.listeners(PersistingEventListener.class)).isEmpty();
    assertThat(registry.listeners(BatchPersistListener.class)).isEmpty();
  }

  @Test
  void getListenersReturnsEmptyAfterAllListenersOfContractAreRemoved() {
    PersistingEventListener<UserModel> listener = new UserEventListening();
    registry.addListener(listener);

    registry.removeListener(listener);

    assertThat(registry.listeners(PersistingEventListener.class)).isEmpty();
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
    PersistingEventListener<UserModel> listener = new PersistingEventListener<>() {
    };

    registry.addListener(listener);
    assertThat(registry.listeners(PersistingEventListener.class)).containsExactly(listener);

    registry.removeListener(listener);
    assertThat(registry.listeners(PersistingEventListener.class)).isEmpty();
  }

  @Test
  void addListenersAcceptsNullCollection() {
    registry.addListeners(null);
    assertThat(registry.listeners(PersistingEventListener.class)).isEmpty();
  }

  @Test
  void removeListenersRemovesMultipleListeners() {
    PersistingEventListener<UserModel> first = new UserEventListening();
    PersistingEventListener<UserModel> second = new UserEventListening();
    registry.addListeners(List.of(first, second));

    registry.removeListeners(List.of(first));

    assertThat(registry.listeners(PersistingEventListener.class)).containsExactly(second);
  }

  @Test
  void clearRemovesAllListenersAcrossContracts() {
    registry.addListener(new UserEventListening());
    registry.addListener((BatchPersistListener) (execution, implicitExecution, exception) -> {
    });

    registry.clear();

    assertThat(registry.listeners(PersistingEventListener.class)).isEmpty();
    assertThat(registry.listeners(BatchPersistListener.class)).isEmpty();
  }

  @Test
  void updatingAndDeletingListenersAreStoredByTheirOwnContract() {
    UpdatingEventListener<UserModel> updating = new UpdatingEventListener<>() {
    };
    DeletingEventListener<UserModel> deleting = new DeletingEventListener<>() {
    };

    registry.addListener(updating);
    registry.addListener(deleting);

    assertThat(registry.listeners(UpdatingEventListener.class)).containsExactly(updating);
    assertThat(registry.listeners(DeletingEventListener.class)).containsExactly(deleting);
  }

  @Test
  void updatingAndDeletingListenersAreRemovedIndependently() {
    UpdatingEventListener<UserModel> updating = new UpdatingEventListener<>() {
    };
    DeletingEventListener<UserModel> deleting = new DeletingEventListener<>() {
    };

    registry.addListener(updating);
    registry.addListener(deleting);

    registry.removeListener(updating);

    assertThat(registry.listeners(UpdatingEventListener.class)).isEmpty();
    assertThat(registry.listeners(DeletingEventListener.class)).containsExactly(deleting);
  }

  @Test
  void listenersOfAnEntityContractAreEntityAwareThroughTheUnifiedLookup() {
    UpdatingEventListener<UserModel> user = new UpdatingUserListener();
    UpdatingEventListener<Object> all = new UpdatingEverythingListener();
    registry.addListeners(List.of(user, all));

    // the group obtained via listeners(...) is entity aware: listenersFor filter
    assertThat(registry.listeners(UpdatingEventListener.class).listenersFor(UserModel.class))
            .containsExactlyInAnyOrder(user, all);
    assertThat(registry.listeners(UpdatingEventListener.class).listenersFor(String.class))
            .containsExactly(all);
  }

  static class UserEventListening implements PersistingEventListener<UserModel> {

  }

  static class UpdatingUserListener implements UpdatingEventListener<UserModel> {

  }

  static class UpdatingEverythingListener implements UpdatingEventListener<Object> {

  }

  static class HybridListener implements PersistingEventListener<UserModel>, BatchPersistListener {

    @Override
    public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    }

    @Override
    public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
    }
  }

}
