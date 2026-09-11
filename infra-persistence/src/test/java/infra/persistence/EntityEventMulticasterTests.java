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

package infra.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityDeleteEvent;
import infra.persistence.event.EntityEventListener;
import infra.persistence.event.EntityLoadEvent;
import infra.persistence.event.EntityPersistEvent;
import infra.persistence.event.EntityTruncateEvent;
import infra.persistence.event.EntityUpdateEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class EntityEventMulticasterTests {

  private DefaultEntityEventRegistry registry;

  private EntityEventMulticaster multicaster;

  private EntityMetadata metadata;

  @BeforeEach
  void setUp() {
    registry = new DefaultEntityEventRegistry();
    multicaster = new EntityEventMulticaster(registry);
    metadata = mock(EntityMetadata.class);
  }

  @Test
  void afterPersistEventIsDispatchedToMatchingListener() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("afterPersist:" + event.getEntity().name);
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("afterPersist:TODAY");
  }

  @Test
  void beforePersistUpdateDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPrePersist(EntityPersistEvent<UserModel> event) {
        received.add("beforePersist");
      }

      @Override
      public void onPreUpdate(EntityUpdateEvent<UserModel> event) {
        received.add("beforeUpdate");
      }

      @Override
      public void onPreDelete(EntityDeleteEvent<UserModel> event) {
        received.add("beforeDelete");
      }
    });

    multicaster.onPrePersist(UserModel.male("TODAY", 10), metadata);
    multicaster.onPreUpdate(UserModel.male("TODAY", 10), metadata);
    multicaster.onPreDelete(UserModel.class, null, 42, metadata);

    assertThat(received).containsExactly("beforePersist", "beforeUpdate", "beforeDelete");
  }

  @Test
  void listenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        userReceived.add("user:" + event.getEntity().name);
      }
    });
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(userReceived).containsExactly("user:TODAY");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new EntityEventListener<Object>() {

      @Override
      public void onPostPersist(EntityPersistEvent<Object> event) {
        allReceived.add("all:" + event.getEntity().getClass().getSimpleName());
      }
    });
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void listenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<Object>() {

      @Override
      public void onPostPersist(EntityPersistEvent<Object> event) {
        received.add("supertype:" + event.getEntity().getClass().getSimpleName());
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void insertUpdateAndDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("persist");
      }

      @Override
      public void onPostUpdate(EntityUpdateEvent<UserModel> event) {
        received.add("update");
      }

      @Override
      public void onPostDelete(EntityDeleteEvent<UserModel> event) {
        received.add("delete");
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata);
    multicaster.onPostDelete(UserModel.class, null, 42, metadata);

    assertThat(received).containsExactly("persist", "update", "delete");
  }

  @Test
  void deleteEventCarriesEntityAndId() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostDelete(EntityDeleteEvent<UserModel> event) {
        received.add("entity=" + (event.getEntity() != null)
                + ",id=" + event.getId() + ",class=" + event.getEntityClass().getName());
      }
    });

    multicaster.onPostDelete(UserModel.class, null, 42, metadata);
    multicaster.onPostDelete(UserModel.class, UserModel.male("TODAY", 10), 7, metadata);

    assertThat(received).containsExactly(
            "entity=false,id=42,class=" + UserModel.class.getName(),
            "entity=true,id=7,class=" + UserModel.class.getName());
  }

  @Test
  void loadAndTruncateEventsAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostLoad(EntityLoadEvent<UserModel> event) {
        received.add("load:" + event.getEntity().name);
      }

      @Override
      public void onPostTruncate(EntityTruncateEvent<UserModel> event) {
        received.add("truncate:" + event.getEntityClass().getSimpleName()
                + ",entity=" + (event.getEntity() == null));
      }
    });

    multicaster.onPostLoad(UserModel.male("TODAY", 10), metadata);
    multicaster.onPostTruncate(UserModel.class, metadata);

    assertThat(received).containsExactly("load:TODAY", "truncate:UserModel,entity=true");
  }

  @Test
  void listenersAreInvokedInOrder() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("first");
      }
    });
    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("second");
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);

    assertThat(received).containsExactly("first", "second");
  }

  @Test
  void listenerObservingInterfaceReceivesImplementorEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new EntityEventListener<NamedEntity>() {

      @Override
      public void onPostPersist(EntityPersistEvent<NamedEntity> event) {
        received.add("named:" + event.getEntity().getName());
      }
    });

    multicaster.onPostPersist(new IEntity("TODAY"), metadata);

    assertThat(received).containsExactly("named:TODAY");
  }

  @Test
  void listenerAddedAfterDispatchIsPickedUp() {
    List<String> received = new ArrayList<>();

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).isEmpty();

    registry.addListener(new EntityEventListener<UserModel>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("late:" + event.getEntity().name);
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).containsExactly("late:TODAY");
  }

  @Test
  void listenerRemovedAfterDispatchIsNoLongerInvoked() {
    List<String> received = new ArrayList<>();

    EntityEventListener<UserModel> listener = new EntityEventListener<>() {

      @Override
      public void onPostPersist(EntityPersistEvent<UserModel> event) {
        received.add("remove:" + event.getEntity().name);
      }
    };
    registry.addListener(listener);

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).containsExactly("remove:TODAY");

    registry.removeListener(listener);
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata);
    assertThat(received).containsExactly("remove:TODAY");
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