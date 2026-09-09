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

import infra.persistence.EntityMetadata;

/**
 * Event fired when an entity has been successfully persisted.
 *
 * <p>The entity instance is fully populated by the time this event is published:
 * auto-generated identifiers (if any) have already been written back onto the
 * entity.
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class EntityInsertEvent<T> extends EntityEvent<T> {

  private final T entity;

  /**
   * Create a new {@code EntityInsertEvent}.
   *
   * @param entity the persisted entity; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  public EntityInsertEvent(T entity, EntityMetadata metadata) {
    super(entity.getClass(), metadata);
    this.entity = entity;
  }

  @Override
  public T getEntity() {
    return this.entity;
  }

  @Override
  public String toString() {
    return "EntityInsertEvent[entity=" + this.entity + ']';
  }

}