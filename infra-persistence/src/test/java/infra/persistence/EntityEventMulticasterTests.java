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
import infra.persistence.event.DeletingEventListener;
import infra.persistence.event.PersistingEventListener;
import infra.persistence.event.PostLoadEventListener;
import infra.persistence.event.PostTruncateEventListener;
import infra.persistence.event.UpdatingEventListener;

import static org.assertj.core.api.Assertions.assertThat;

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
    metadata = new DefaultEntityMetadataFactory().getEntityMetadata(UserModel.class);
  }

  @Test
  void afterPersistEventIsDispatchedToMatchingListener() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("afterPersist:" + entity.name);
      }
    });

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("afterPersist:TODAY");
  }

  @Test
  void beforePersistUpdateDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPrePersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("beforePersist");
      }
    });
    registry.addListener(new UpdatingEventListener<UserModel>() {

      @Override
      public void onPreUpdating(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("beforeUpdate");
      }
    });
    registry.addListener(new DeletingEventListener<UserModel>() {

      @Override
      public void onPreDeleting(UserModel entity, Object id, EntityMetadata metadata) {
        received.add("beforeDelete");
      }
    });

    multicaster.onPrePersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPreUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPreDeleting(null, 42, metadata);

    assertThat(received).containsExactly("beforePersist", "beforeUpdate", "beforeDelete");
  }

  @Test
  void listenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        userReceived.add("user:" + entity.name);
      }
    });
    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(userReceived).containsExactly("user:TODAY");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new PersistingEventListener<Object>() {

      @Override
      public void onPostPersisting(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        allReceived.add("all:" + entity.getClass().getSimpleName());
      }
    });
    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void listenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<Object>() {

      @Override
      public void onPostPersisting(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("supertype:" + entity.getClass().getSimpleName());
      }
    });

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void insertUpdateAndDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("persist");
      }
    });
    registry.addListener(new UpdatingEventListener<UserModel>() {

      @Override
      public void onPostUpdating(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("update");
      }
    });
    registry.addListener(new DeletingEventListener<UserModel>() {

      @Override
      public void onPostDeleting(UserModel entity, Object id, EntityMetadata metadata) {
        received.add("delete");
      }
    });

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPostUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPostDeleting(null, 42, metadata);

    assertThat(received).containsExactly("persist", "update", "delete");
  }

  @Test
  void deleteEventCarriesEntityAndId() {
    List<String> received = new ArrayList<>();

    registry.addListener(new DeletingEventListener<UserModel>() {

      @Override
      public void onPostDeleting(UserModel entity, Object id, EntityMetadata meta) {
        received.add("entity=" + (entity != null)
                + ",id=" + id + ",class=" + meta.entityClass.getName());
      }
    });

    multicaster.onPostDeleting(null, 42, metadata);
    multicaster.onPostDeleting(UserModel.male("TODAY", 10), 7, metadata);

    assertThat(received).containsExactly(
            "entity=false,id=42,class=" + UserModel.class.getName(),
            "entity=true,id=7,class=" + UserModel.class.getName());
  }

  @Test
  void updatingListenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new UpdatingEventListener<UserModel>() {

      @Override
      public void onPostUpdating(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        userReceived.add("user:" + entity.name);
      }
    });
    multicaster.onPostUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(userReceived).containsExactly("user:TODAY");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new UpdatingEventListener<Object>() {

      @Override
      public void onPostUpdating(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        allReceived.add("all:" + entity.getClass().getSimpleName());
      }
    });
    multicaster.onPostUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void updatingListenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new UpdatingEventListener<Object>() {

      @Override
      public void onPreUpdating(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("supertype:" + entity.getClass().getSimpleName());
      }
    });

    multicaster.onPreUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void deletingListenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new DeletingEventListener<UserModel>() {

      @Override
      public void onPostDeleting(UserModel entity, Object id, EntityMetadata metadata) {
        userReceived.add("user:" + id);
      }
    });
    multicaster.onPostDeleting(null, 42, metadata);
    assertThat(userReceived).containsExactly("user:42");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new DeletingEventListener<Object>() {

      @Override
      public void onPostDeleting(Object entity, Object id, EntityMetadata metadata) {
        allReceived.add("all:" + id);
      }
    });
    multicaster.onPostDeleting(null, 7, metadata);
    assertThat(allReceived).containsExactly("all:7");
  }

  @Test
  void updateAndDeleteListenersAreIsolatedFromPersist() {
    List<String> received = new ArrayList<>();

    registry.addListener(new UpdatingEventListener<UserModel>() {

      @Override
      public void onPostUpdating(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("update");
      }
    });
    registry.addListener(new DeletingEventListener<UserModel>() {

      @Override
      public void onPostDeleting(UserModel entity, Object id, EntityMetadata metadata) {
        received.add("delete");
      }
    });

    // persist must not reach update/delete listeners
    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).isEmpty();

    multicaster.onPostUpdating(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPostDeleting(null, 42, metadata);

    assertThat(received).containsExactly("update", "delete");
  }

  @Test
  void loadAndTruncateEventsAreDispatched() {
    List<String> received = new ArrayList<>();

    class Listener0 implements PostTruncateEventListener, PostLoadEventListener<UserModel> {

      @Override
      public void onPostLoad(UserModel entity, EntityMetadata metadata) {
        received.add("load:" + entity.name);
      }

      @Override
      public void onPostTruncate(Class<?> entityClass, EntityMetadata metadata) {
        received.add("truncate:" + entityClass.getSimpleName());
      }
    }

    registry.addListener(new Listener0());

    multicaster.onPostLoad(UserModel.male("TODAY", 10), metadata);
    multicaster.onPostTruncate(UserModel.class, metadata);

    assertThat(received).containsExactly("load:TODAY", "truncate:UserModel");
  }

  @Test
  void listenersAreInvokedInOrder() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("first");
      }
    });
    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("second");
      }
    });

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("first", "second");
  }

  @Test
  void listenerObservingInterfaceReceivesImplementorEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistingEventListener<NamedEntity>() {

      @Override
      public void onPostPersisting(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("named:" + entity.getName());
      }
    });

    multicaster.onPostPersisting(new IEntity("TODAY"), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("named:TODAY");
  }

  @Test
  void listenerAddedAfterDispatchIsPickedUp() {
    List<String> received = new ArrayList<>();

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).isEmpty();

    registry.addListener(new PersistingEventListener<UserModel>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("late:" + entity.name);
      }
    });

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).containsExactly("late:TODAY");
  }

  @Test
  void listenerRemovedAfterDispatchIsNoLongerInvoked() {
    List<String> received = new ArrayList<>();

    PersistingEventListener<UserModel> listener = new PersistingEventListener<>() {

      @Override
      public void onPostPersisting(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("remove:" + entity.name);
      }
    };
    registry.addListener(listener);

    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).containsExactly("remove:TODAY");

    registry.removeListener(listener);
    multicaster.onPostPersisting(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
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
