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
import infra.persistence.event.DeleteEventListener;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.PersistEventListener;
import infra.persistence.event.PostLoadEventListener;
import infra.persistence.event.PostTruncateEventListener;
import infra.persistence.event.UpdateEventListener;

/**
 * Package-private multicast for entity lifecycle events. It is owned by the
 * {@link DefaultEntityManager} and dispatched against the {@link EntityEventRegistry}
 * it was created with.
 *
 * <p>It keeps the dispatch concern out of the public registry contract without
 * exposing an additional API: the manager uses it to fire
 * {@code before/after persist/update/delete} events as well as their
 * {@code failed} variants. The matching listeners are
 * resolved and cached by the {@link EntityEventRegistry} per entity class.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultEntityEventRegistry
 * @since 5.0
 */
@SuppressWarnings("unchecked")
final class EntityEventMulticaster {

  private final EntityEventRegistry registry;

  public EntityEventMulticaster(EntityEventRegistry registry) {
    this.registry = registry;
  }

  public void onPrePersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    for (var listener : registry.listeners(PersistEventListener.class).listenersFor(entity.getClass())) {
      listener.onPrePersist(entity, metadata, strategy);
    }
  }

  public void onPostPersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    for (var listener : registry.listeners(PersistEventListener.class).listenersFor(entity.getClass())) {
      listener.onPostPersist(entity, metadata, strategy);
    }
  }

  public void onPersistFailed(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, Throwable exception) {
    for (var listener : registry.listeners(PersistEventListener.class).listenersFor(entity.getClass())) {
      listener.onPersistFailed(entity, metadata, strategy, exception);
    }
  }

  public void onPreUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    for (var listener : registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass())) {
      listener.onPreUpdate(entity, metadata, strategy);
    }
  }

  public void onPostUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    for (var listener : registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass())) {
      listener.onPostUpdate(entity, metadata, strategy);
    }
  }

  public void onUpdateFailed(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, Throwable exception) {
    for (var listener : registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass())) {
      listener.onUpdateFailed(entity, metadata, strategy, exception);
    }
  }

  public void onPreDelete(@Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(metadata.entityClass)) {
      listener.onPreDelete(entity, id, metadata);
    }
  }

  public void onPostDelete(@Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(metadata.entityClass)) {
      listener.onPostDelete(entity, id, metadata);
    }
  }

  public void onDeleteFailed(@Nullable Object entity, @Nullable Object id, EntityMetadata metadata, Throwable exception) {
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(metadata.entityClass)) {
      listener.onDeleteFailed(entity, id, metadata, exception);
    }
  }

  public void onPostLoad(Object entity, EntityMetadata metadata) {
    for (var listener : registry.listeners(PostLoadEventListener.class).listenersFor(entity.getClass())) {
      listener.onPostLoad(entity, metadata);
    }
  }

  public void onPostTruncate(Class<?> entityClass, EntityMetadata metadata) {
    for (var listener : registry.listeners(PostTruncateEventListener.class)) {
      listener.onPostTruncate(entityClass, metadata);
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