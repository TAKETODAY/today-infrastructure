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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityDeleteEvent;
import infra.persistence.event.EntityEventListener;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.EntityPersistEvent;
import infra.persistence.event.EntityUpdateEvent;

/**
 * Package-private multicast for {@link infra.persistence.event.EntityEvent entity
 * lifecycle events}. It is owned by the {@link DefaultEntityManager} and dispatched
 * against the {@link EntityEventRegistry} it was created with.
 *
 * <p>It keeps the dispatch concern out of the public registry contract without
 * exposing an additional API: the manager uses it to fire
 * {@code before/after persist/update/delete} events. The matching listeners are
 * resolved and cached by the {@link EntityEventRegistry} per entity class.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultEntityEventRegistry
 * @since 5.0
 */
final class DefaultEntityEventMulticaster {

  private final EntityEventRegistry registry;

  DefaultEntityEventMulticaster(EntityEventRegistry registry) {
    this.registry = registry;
  }

  void publishBeforePersist(Object entity, EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entity.getClass());
    if (listeners.isEmpty()) {
      return;
    }
    EntityPersistEvent<Object> event = new EntityPersistEvent<>(entity, metadata);
    for (EntityEventListener listener : listeners) {
      listener.beforePersist(event);
    }
  }

  void publishAfterPersist(Object entity, EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entity.getClass());
    if (listeners.isEmpty()) {
      return;
    }
    EntityPersistEvent<Object> event = new EntityPersistEvent<>(entity, metadata);
    for (EntityEventListener listener : listeners) {
      listener.afterPersist(event);
    }
  }

  void publishBeforeUpdate(Object entity, EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entity.getClass());
    if (listeners.isEmpty()) {
      return;
    }
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (EntityEventListener listener : listeners) {
      listener.beforeUpdate(event);
    }
  }

  void publishAfterUpdate(Object entity, EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entity.getClass());
    if (listeners.isEmpty()) {
      return;
    }
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (EntityEventListener listener : listeners) {
      listener.afterUpdate(event);
    }
  }

  void publishBeforeDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id,
          EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entityClass);
    if (listeners.isEmpty()) {
      return;
    }
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (EntityEventListener listener : listeners) {
      listener.beforeDelete(event);
    }
  }

  void publishAfterDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    List<EntityEventListener<?>> listeners = matchingListeners(entityClass);
    if (listeners.isEmpty()) {
      return;
    }
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (EntityEventListener listener : listeners) {
      listener.afterDelete(event);
    }
  }

  /**
   * Return the listeners matching the given entity class, resolved and cached by
   * the {@link EntityEventRegistry}.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List<EntityEventListener<?>> matchingListeners(Class<?> entityClass) {
    return (List) registry.matchingListeners(EntityEventListener.class, entityClass);
  }

}