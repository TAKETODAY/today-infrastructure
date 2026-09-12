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
 * Listener for entity <strong>load</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener} — the common base contract
 * shared by the entity lifecycle listeners — and models only the load concern via
 * {@link #onPostLoad(Object, EntityMetadata)}. To observe other lifecycle
 * operations, implement the corresponding contract, e.g. {@link PersistingEventListener}
 * or {@link DeletingEventListener}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code PostLoadEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} load events. A listener whose generic type cannot be
 * resolved observes every entity.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link infra.persistence.EntityManager} right after an entity has been loaded
 * from the data store; an exception thrown by a listener therefore propagates to
 * the caller.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityEventListener
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface PostLoadEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked after an entity of the observed type was loaded from the data store.
   *
   * <p>The entity is fully populated from the {@code ResultSet} by the time this
   * callback is invoked. It is fired for every entity returned by a query
   * (find / findById / iterate / page / count-derived reads). Listeners may modify
   * the entity before it is handed to the caller, e.g. to populate derived fields
   * or feed a cache.
   *
   * @param entity the loaded entity; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  void onPostLoad(T entity, EntityMetadata metadata);
}
