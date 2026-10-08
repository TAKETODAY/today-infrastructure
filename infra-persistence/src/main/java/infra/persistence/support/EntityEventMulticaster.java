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

package infra.persistence.support;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.BatchExecutionListener;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.DeleteEventListener;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.EntityFailureContext;
import infra.persistence.event.EntityOperationPhase;
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

  public PropertyUpdateStrategy onPrePersist(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    var listeners = registry.listeners(PersistEventListener.class).listenersFor(entity.getClass());
    for (var listener : listeners) {
      strategy = listener.onPrePersist(entity, metadata, strategy);
    }
    return strategy;
  }

  public void onPostPersist(Object entity, EntityMetadata metadata, List<EntityProperty> properties) {
    for (var listener : registry.listeners(PersistEventListener.class).listenersFor(entity.getClass())) {
      listener.onPostPersist(entity, metadata, properties);
    }
  }

  public void onPersistFailed(Object entity, EntityMetadata metadata,
          EntityOperationPhase phase, @Nullable List<EntityProperty> properties, Throwable exception) {
    Object id = null;
    try {
      EntityProperty idProperty = metadata.getIdProperty();
      if (idProperty != null) {
        id = idProperty.getValue(entity);
      }
    }
    catch (Throwable ex) {
      if (ex != exception) {
        exception.addSuppressed(ex);
      }
    }
    onPersistFailed(entity, new EntityFailureContext(metadata, phase, properties, id, exception));
  }

  public void onPersistFailed(Object entity, EntityFailureContext context) {
    Throwable exception = context.getException();
    for (var listener : registry.listeners(PersistEventListener.class).listenersFor(entity.getClass())) {
      try {
        listener.onPersistFailed(entity, context);
      }
      catch (Throwable ex) {
        if (exception != ex) {
          exception.addSuppressed(ex);
        }
      }
    }
  }

  public PropertyUpdateStrategy onPreUpdate(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    var listeners = registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass());
    for (var listener : listeners) {
      strategy = listener.onPreUpdate(entity, metadata, strategy);
    }
    return strategy;
  }

  public void onPostUpdate(Object entity, EntityMetadata metadata, List<EntityProperty> properties, int affectedRows) {
    for (var listener : registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass())) {
      listener.onPostUpdate(entity, metadata, properties, affectedRows);
    }
  }

  public void onUpdateFailed(Object entity, EntityMetadata metadata,
          EntityOperationPhase phase, @Nullable List<EntityProperty> properties, @Nullable Object id, Throwable exception) {
    onUpdateFailed(entity, new EntityFailureContext(metadata, phase, properties, id, exception));
  }

  public void onUpdateFailed(Object entity, EntityFailureContext context) {
    Throwable exception = context.getException();
    for (var listener : registry.listeners(UpdateEventListener.class).listenersFor(entity.getClass())) {
      try {
        listener.onUpdateFailed(entity, context);
      }
      catch (Throwable ex) {
        if (ex != exception) {
          exception.addSuppressed(ex);
        }
      }
    }
  }

  public void onPreDelete(@Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(metadata.getEntityClass())) {
      listener.onPreDelete(entity, id, metadata);
    }
  }

  public void onPostDelete(@Nullable Object entity, @Nullable Object id, EntityMetadata metadata, int affectedRows) {
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(metadata.getEntityClass())) {
      listener.onPostDelete(entity, id, metadata, affectedRows);
    }
  }

  public void onDeleteFailed(@Nullable Object entity, EntityMetadata metadata,
          EntityOperationPhase phase, @Nullable Object id, Throwable exception) {
    onDeleteFailed(entity, new EntityFailureContext(metadata, phase, null, id, exception));
  }

  public void onDeleteFailed(@Nullable Object entity, EntityFailureContext context) {
    Throwable exception = context.getException();
    for (var listener : registry.listeners(DeleteEventListener.class).listenersFor(context.getMetadata().getEntityClass())) {
      try {
        listener.onDeleteFailed(entity, context);
      }
      catch (Throwable ex) {
        if (exception != ex) {
          exception.addSuppressed(ex);
        }
      }
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
    for (BatchExecutionListener listener : registry.listeners(BatchExecutionListener.class)) {
      listener.preProcessing(execution, implicitExecution);
    }
  }

  public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
    Throwable failure = exception;
    for (BatchExecutionListener listener : registry.listeners(BatchExecutionListener.class)) {
      try {
        listener.postProcessing(execution, implicitExecution, exception);
      }
      catch (RuntimeException | Error ex) {
        if (failure == null) {
          failure = ex;
        }
        else if (failure != ex) {
          failure.addSuppressed(ex);
        }
      }
    }
    if (exception == null) {
      if (failure instanceof RuntimeException ex) {
        throw ex;
      }
      if (failure instanceof Error ex) {
        throw ex;
      }
    }
  }

}
