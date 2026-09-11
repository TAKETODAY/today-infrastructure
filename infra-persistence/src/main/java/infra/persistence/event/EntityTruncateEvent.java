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
 * Event fired after the table of an entity type was truncated.
 *
 * <p>Truncation operates on the whole table, so no entity instance is available:
 * {@link #getEntity()} always returns {@code null}. Listeners can react upon the
 * entity class via {@link #getEntityClass()} and its {@link #getEntityMetadata()
 * metadata}.
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityEventListener#onPostTruncate
 * @since 5.0
 */
public final class EntityTruncateEvent<T> extends EntityEvent<T> {

  private final Class<?> entityClass;

  /**
   * Create a new {@code EntityTruncateEvent}.
   *
   * @param entityClass the truncated entity class; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  public EntityTruncateEvent(Class<?> entityClass, EntityMetadata metadata) {
    super(entityClass, metadata);
    this.entityClass = entityClass;
  }

  @Override
  public @Nullable T getEntity() {
    return null;
  }

  @Override
  public String toString() {
    return "EntityTruncateEvent[entityClass=" + entityClass + ']';
  }

}
