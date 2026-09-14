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
import infra.persistence.event.DeleteEventListener;
import infra.persistence.event.PersistEventListener;
import infra.persistence.event.PostLoadEventListener;
import infra.persistence.event.PostTruncateEventListener;
import infra.persistence.event.UpdateEventListener;

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

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("afterPersist:" + entity.name);
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("afterPersist:TODAY");
  }

  @Test
  void beforePersistUpdateDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPrePersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("beforePersist");
      }
    });
    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onPreUpdate(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("beforeUpdate");
      }
    });
    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onPreDelete(UserModel entity, Object id, EntityMetadata metadata) {
        received.add("beforeDelete");
      }
    });

    multicaster.onPrePersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPreUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPreDelete(null, 42, metadata);

    assertThat(received).containsExactly("beforePersist", "beforeUpdate", "beforeDelete");
  }

  @Test
  void listenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        userReceived.add("user:" + entity.name);
      }
    });
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(userReceived).containsExactly("user:TODAY");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new PersistEventListener<Object>() {

      @Override
      public void onPostPersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        allReceived.add("all:" + entity.getClass().getSimpleName());
      }
    });
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void listenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistEventListener<Object>() {

      @Override
      public void onPostPersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("supertype:" + entity.getClass().getSimpleName());
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void insertUpdateAndDeleteAreDispatched() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("persist");
      }
    });
    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onPostUpdate(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, int affectedRows) {
        received.add("update");
      }
    });
    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onPostDelete(UserModel entity, Object id, EntityMetadata metadata, int affectedRows) {
        received.add("delete");
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), 1);
    multicaster.onPostDelete(null, 42, metadata, 1);

    assertThat(received).containsExactly("persist", "update", "delete");
  }

  @Test
  void postUpdateEventCarriesAffectedRows() {
    List<Integer> received = new ArrayList<>();

    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onPostUpdate(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, int affectedRows) {
        received.add(affectedRows);
      }
    });

    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), 3);

    assertThat(received).containsExactly(3);
  }

  @Test
  void deleteEventCarriesEntityAndId() {
    List<String> received = new ArrayList<>();

    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onPostDelete(UserModel entity, Object id, EntityMetadata meta, int affectedRows) {
        received.add("entity=" + (entity != null)
                + ",id=" + id + ",class=" + meta.getEntityClass().getName());
      }
    });

    multicaster.onPostDelete(null, 42, metadata, 1);
    multicaster.onPostDelete(UserModel.male("TODAY", 10), 7, metadata, 1);

    assertThat(received).containsExactly(
            "entity=false,id=42,class=" + UserModel.class.getName(),
            "entity=true,id=7,class=" + UserModel.class.getName());
  }

  @Test
  void updatingListenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onPostUpdate(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, int affectedRows) {
        userReceived.add("user:" + entity.name);
      }
    });
    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), 1);
    assertThat(userReceived).containsExactly("user:TODAY");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new UpdateEventListener<Object>() {

      @Override
      public void onPostUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, int affectedRows) {
        allReceived.add("all:" + entity.getClass().getSimpleName());
      }
    });
    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), 1);
    assertThat(allReceived).containsExactly("all:UserModel");
  }

  @Test
  void updatingListenerObservingSupertypeReceivesSubtypeEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new UpdateEventListener<Object>() {

      @Override
      public void onPreUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("supertype:" + entity.getClass().getSimpleName());
      }
    });

    multicaster.onPreUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("supertype:UserModel");
  }

  @Test
  void deletingListenerIsFilteredByDeclaredGenericType() {
    List<String> userReceived = new ArrayList<>();
    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onPostDelete(UserModel entity, Object id, EntityMetadata metadata, int affectedRows) {
        userReceived.add("user:" + id);
      }
    });
    multicaster.onPostDelete(null, 42, metadata, 1);
    assertThat(userReceived).containsExactly("user:42");

    List<String> allReceived = new ArrayList<>();
    registry.addListener(new DeleteEventListener<Object>() {

      @Override
      public void onPostDelete(Object entity, Object id, EntityMetadata metadata, int affectedRows) {
        allReceived.add("all:" + id);
      }
    });
    multicaster.onPostDelete(null, 7, metadata, 1);
    assertThat(allReceived).containsExactly("all:7");
  }

  @Test
  void updateAndDeleteListenersAreIsolatedFromPersist() {
    List<String> received = new ArrayList<>();

    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onPostUpdate(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, int affectedRows) {
        received.add("update");
      }
    });
    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onPostDelete(UserModel entity, Object id, EntityMetadata metadata, int affectedRows) {
        received.add("delete");
      }
    });

    // persist must not reach update/delete listeners
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).isEmpty();

    multicaster.onPostUpdate(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), 1);
    multicaster.onPostDelete(null, 42, metadata, 1);

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

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("first");
      }
    });
    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("second");
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("first", "second");
  }

  @Test
  void listenerObservingInterfaceReceivesImplementorEvents() {
    List<String> received = new ArrayList<>();

    registry.addListener(new PersistEventListener<NamedEntity>() {

      @Override
      public void onPostPersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("named:" + entity.getName());
      }
    });

    multicaster.onPostPersist(new IEntity("TODAY"), metadata, PropertyUpdateStrategy.noneNull());

    assertThat(received).containsExactly("named:TODAY");
  }

  @Test
  void listenerAddedAfterDispatchIsPickedUp() {
    List<String> received = new ArrayList<>();

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).isEmpty();

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("late:" + entity.name);
      }
    });

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).containsExactly("late:TODAY");
  }

  @Test
  void listenerRemovedAfterDispatchIsNoLongerInvoked() {
    List<String> received = new ArrayList<>();

    PersistEventListener<UserModel> listener = new PersistEventListener<>() {

      @Override
      public void onPostPersist(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        received.add("remove:" + entity.name);
      }
    };
    registry.addListener(listener);

    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).containsExactly("remove:TODAY");

    registry.removeListener(listener);
    multicaster.onPostPersist(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull());
    assertThat(received).containsExactly("remove:TODAY");
  }

  @Test
  void failureEventsCarryException() {
    List<String> received = new ArrayList<>();
    RuntimeException failure = new IllegalStateException("boom");

    registry.addListener(new PersistEventListener<UserModel>() {

      @Override
      public void onPersistFailed(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, Throwable exception) {
        received.add("persist:" + exception.getMessage());
      }
    });
    registry.addListener(new UpdateEventListener<UserModel>() {

      @Override
      public void onUpdateFailed(UserModel entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, Throwable exception) {
        received.add("update:" + exception.getMessage());
      }
    });
    registry.addListener(new DeleteEventListener<UserModel>() {

      @Override
      public void onDeleteFailed(UserModel entity, Object id, EntityMetadata metadata, Throwable exception) {
        received.add("delete:" + exception.getMessage());
      }
    });

    multicaster.onPersistFailed(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), failure);
    multicaster.onUpdateFailed(UserModel.male("TODAY", 10), metadata, PropertyUpdateStrategy.noneNull(), failure);
    multicaster.onDeleteFailed(null, 42, metadata, failure);

    assertThat(received).containsExactly("persist:boom", "update:boom", "delete:boom");
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
