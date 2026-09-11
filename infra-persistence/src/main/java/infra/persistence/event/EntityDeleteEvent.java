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
 * Represents the lifecycle of a single entity delete operation. It is passed to
 * listeners both before the entity is deleted
 * ({@link EntityEventListener#beforeDelete}) and after it has been deleted
 * ({@link EntityEventListener#afterDelete}).
 *
 * <p>When the entity is deleted by instance or example, the {@linkplain #getEntity()
 * entity} is available. When deleted by id only, no entity instance exists and the
 * {@linkplain #getId() id} (if known) is provided instead.
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityEventListener#beforeDelete
 * @see EntityEventListener#afterDelete
 * @since 5.0
 */
public final class EntityDeleteEvent<T> extends EntityEvent<T> {

  private final @Nullable T entity;

  private final @Nullable Object id;

  /**
   * Create a new {@code EntityDeleteEvent}.
   *
   * @param entityClass the entity class; must not be {@code null}
   * @param entity the deleted entity, or {@code null} if not available
   * @param id the deleted id, or {@code null} if not available
   * @param metadata the entity metadata; must not be {@code null}
   */
  public EntityDeleteEvent(Class<?> entityClass, @Nullable T entity, @Nullable Object id, EntityMetadata metadata) {
    super(entityClass, metadata);
    this.entity = entity;
    this.id = id;
  }

  /**
   * Return the deleted entity instance, or {@code null} if the entity was deleted by
   * id and no instance was available.
   */
  @Override
  public @Nullable T getEntity() {
    return this.entity;
  }

  /**
   * Return the id of the deleted entity, or {@code null} if not available (e.g. when
   * deleting by example).
   */
  public @Nullable Object getId() {
    return this.id;
  }

  @Override
  public String toString() {
    return "EntityDeleteEvent[entityClass=" + getEntityClass() + ", entity=" + this.entity + ']';
  }

}