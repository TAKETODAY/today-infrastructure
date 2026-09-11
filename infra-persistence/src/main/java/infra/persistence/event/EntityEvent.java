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

import infra.persistence.EntityMetadata;

/**
 * Base class for entity lifecycle events.
 *
 * <p>An {@link EntityEvent} carries the target entity {@linkplain #getEntityClass() class}
 * together with its {@linkplain #getEntityMetadata() metadata} so that listeners can
 * react upon the lifecycle of a specific entity without inspecting the object graph.
 *
 * <p>Events are dispatched <strong>synchronously</strong> by the
 * {@link infra.persistence.EntityManager} right after the underlying statement has
 * completed. Listener exceptions therefore propagate to the caller and may abort the
 * surrounding operation.
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityPersistEvent
 * @see infra.persistence.event.EntityUpdateEvent
 * @see infra.persistence.event.EntityDeleteEvent
 * @since 5.0
 */
public abstract class EntityEvent<T> {

  private final Class<?> entityClass;

  private final EntityMetadata metadata;

  /**
   * Create a new {@code EntityEvent}.
   *
   * @param entityClass the entity class; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  protected EntityEvent(Class<?> entityClass, EntityMetadata metadata) {
    this.entityClass = entityClass;
    this.metadata = metadata;
  }

  /**
   * Return the class of the entity this event is associated with. For
   * {@link infra.persistence.event.EntityPersistEvent} and
   * {@link infra.persistence.event.EntityUpdateEvent} this is the runtime class of
   * the {@linkplain #getEntity() entity}; for
   * {@link infra.persistence.event.EntityDeleteEvent} this is the entity class the
   * delete operation was executed against, which may differ from the class of the
   * (possibly {@code null}) entity instance.
   */
  public final Class<?> getEntityClass() {
    return this.entityClass;
  }

  /**
   * Return the metadata of the entity this event is associated with.
   */
  public final EntityMetadata getEntityMetadata() {
    return this.metadata;
  }

  /**
   * Return the entity instance this event is associated with, or {@code null} if not
   * available (e.g. an entity deleted by id only).
   */
  public abstract @Nullable T getEntity();

}