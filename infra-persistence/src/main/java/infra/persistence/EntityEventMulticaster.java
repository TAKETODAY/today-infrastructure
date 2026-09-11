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

import infra.persistence.event.BatchExecution;
import infra.persistence.event.BatchPersistListener;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityDeleteEvent;
import infra.persistence.event.EntityEventListener;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.EntityLoadEvent;
import infra.persistence.event.EntityPersistEvent;
import infra.persistence.event.EntityTruncateEvent;
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
@SuppressWarnings("unchecked")
final class EntityEventMulticaster {

  private final EntityEventRegistry registry;

  EntityEventMulticaster(EntityEventRegistry registry) {
    this.registry = registry;
  }

  void onPrePersist(Object entity, EntityMetadata metadata) {
    EntityPersistEvent<Object> event = new EntityPersistEvent<>(entity, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entity.getClass())) {
      listener.onPrePersist(event);
    }
  }

  void onPostPersist(Object entity, EntityMetadata metadata) {
    EntityPersistEvent<Object> event = new EntityPersistEvent<>(entity, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entity.getClass())) {
      listener.onPostPersist(event);
    }
  }

  void onPreUpdate(Object entity, EntityMetadata metadata) {
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entity.getClass())) {
      listener.onPreUpdate(event);
    }
  }

  void onPostUpdate(Object entity, EntityMetadata metadata) {
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entity.getClass())) {
      listener.onPostUpdate(event);
    }
  }

  void onPreDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entityClass)) {
      listener.onPreDelete(event);
    }
  }

  void onPostDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entityClass)) {
      listener.onPostDelete(event);
    }
  }

  void onPostLoad(Object entity, EntityMetadata metadata) {
    EntityLoadEvent<Object> event = new EntityLoadEvent<>(entity, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entity.getClass())) {
      listener.onPostLoad(event);
    }
  }

  void onPostTruncate(Class<?> entityClass, EntityMetadata metadata) {
    EntityTruncateEvent<Object> event = new EntityTruncateEvent<>(entityClass, metadata);
    for (var listener : registry.listeners(EntityEventListener.class).matchingListeners(entityClass)) {
      listener.onPostTruncate(event);
    }
  }

  public void preProcessing(BatchExecution execution, boolean implicitExecution) {
    for (BatchPersistListener listener : registry.listeners(BatchPersistListener.class)) {
      listener.preProcessing(execution, implicitExecution);
    }
  }

  public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
    for (BatchPersistListener listener : registry.listeners(BatchPersistListener.class)) {
      listener.postProcessing(execution, implicitExecution, exception);
    }
  }

}