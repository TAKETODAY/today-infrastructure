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
 * Listener for entity <strong>persist</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener}, so an implementation
 * observes the full entity lifecycle through the inherited callbacks in addition
 * to reacting upon persistence via {@link #onPrePersisting(Object, EntityMetadata)} and
 * {@link #onPostPersisting(Object, EntityMetadata)}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code PersistingEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} persist events. A listener whose generic type cannot be
 * resolved observes every entity.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link infra.persistence.EntityManager}; an exception thrown by a listener
 * therefore propagates to the caller.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityEventListener
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface PersistingEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is persisted, before the insert
   * statement is built and executed. Listeners may modify the entity; the changes
   * are picked up by the persistence operation (generated identifiers are not yet
   * assigned at this point).
   *
   * @param entity the entity to be persisted; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  default void onPrePersisting(T entity, EntityMetadata metadata) {
  }

  /**
   * Invoked after an entity of the observed type was successfully persisted.
   *
   * <p>The entity instance is fully populated by the time this callback is invoked:
   * auto-generated identifiers (if any) have already been written back onto the
   * entity.
   *
   * @param entity the fully populated entity; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  default void onPostPersisting(T entity, EntityMetadata metadata) {
  }

}
