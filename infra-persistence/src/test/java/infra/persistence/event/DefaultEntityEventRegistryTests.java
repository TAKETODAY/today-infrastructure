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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.EntityMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DefaultEntityEventRegistryTests {

  private DefaultEntityEventRegistry registry;

  private EntityMetadata metadata;

  @BeforeEach
  void setUp() {
    registry = new DefaultEntityEventRegistry();
    metadata = mock(EntityMetadata.class);
  }

  @Test
  void insertEventIsDispatchedToGenericListener() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("insert:" + event.getEntity().name);
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("insert:TODAY");
  }

  @Test
  void listenerIsFilteredByDeclaredGenericType() {
    List<String> received = new ArrayList<>();

    // listens only to UserModel, "not-an-entity" must not be received
    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("user:" + event.getEntity().name);
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).containsExactly("user:TODAY");

    // generic type can not be resolved -> observes every entity
    List<String> allReceived = new ArrayList<>();
    registry.addListener(new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<Object> event) {
        allReceived.add("all:" + event.getEntity().getClass().getSimpleName());
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void listenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<Object>() {

      @Override
      public void onPersist(EntityPersistEvent<Object> event) {
        received.add("supertype:" + event.getEntity().getClass().getSimpleName());
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void insertUpdateAndDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("insert");
      }

      @Override
      public void onUpdate(EntityUpdateEvent<UserModel> event) {
        received.add("update");
      }

      @Override
      public void onDelete(EntityDeleteEvent<UserModel> event) {
        received.add("delete");
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    registry.publishUpdate(UserModel.male("TODAY", 10), metadata);
    registry.publishDelete(UserModel.class, null, 42, metadata);

    assertThat(received).containsExactly("insert", "update", "delete");
  }

  @Test
  void deleteEventCarriesEntityAndId() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onDelete(EntityDeleteEvent<UserModel> event) {
        received.add("entity=" + (event.getEntity() != null)
                + ",id=" + event.getId() + ",class=" + event.getEntityClass().getName());
      }
    });

    registry.publishDelete(UserModel.class, null, 42, metadata);
    registry.publishDelete(UserModel.class, UserModel.male("TODAY", 10), 7, metadata);

    assertThat(received).containsExactly(
            "entity=false,id=42,class=" + UserModel.class.getName(),
            "entity=true,id=7,class=" + UserModel.class.getName());
  }

  @Test
  void listenersAreInvokedInOrder() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("first");
      }
    });

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("second");
      }
    });

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("first", "second");
  }

  @Test
  void listenerObservingInterfaceReceivesImplementorEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<NamedEntity>() {

      @Override
      public void onPersist(EntityPersistEvent<NamedEntity> event) {
        received.add("named:" + event.getEntity().getName());
      }
    });

    registry.publishPersist(new IEntity("TODAY"), metadata);

    assertThat(received).containsExactly("named:TODAY");
  }

  @Test
  void removeAndClearTakeEffect() {
    List<String> received = new ArrayList<>();

    EntityEventListener<UserModel> listener = new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("insert");
      }
    };

    registry.addListener(listener);
    registry.removeListener(listener);
    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).isEmpty();

    registry.addListener(listener);
    registry.clear();
    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).isEmpty();
  }

  @Test
  void batchPersistListenersAreManagedByRegistry() {
    BatchPersistListener listener = (execution, implicitExecution, exception) -> {
    };

    registry.addListener(listener);
    assertThat(registry.getListeners(BatchPersistListener.class)).containsExactly(listener);

    registry.setListeners(List.of(listener));
    assertThat(registry.getListeners(BatchPersistListener.class)).containsExactly(listener);

    registry.setListeners(null);
    assertThat(registry.getListeners(BatchPersistListener.class)).isEmpty();
  }

  @Test
  void setListenersReplacesAll() {
    List<String> received = new ArrayList<>();

    EntityEventListener<UserModel> first = new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("first");
      }
    };
    EntityEventListener<UserModel> second = new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("second");
      }
    };

    registry.addListener(first);
    registry.setListeners(List.of(second));

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).containsExactly("second");
  }

  @Test
  void removeListenersRemovesAll() {
    List<String> received = new ArrayList<>();

    EntityEventListener<UserModel> first = new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("first");
      }
    };
    EntityEventListener<UserModel> second = new EntityEventListener<>() {

      @Override
      public void onPersist(EntityPersistEvent<UserModel> event) {
        received.add("second");
      }
    };

    registry.addListener(first);
    registry.addListener(second);
    registry.removeListeners(List.of(first, second));

    registry.publishPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).isEmpty();
  }

  interface NamedEntity {

    String getName();
  }

  static class IEntity implements NamedEntity {

    private final String name;

    IEntity(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }

}