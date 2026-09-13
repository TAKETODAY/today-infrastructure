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
 * Listener for entity <strong>delete</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener} — the common base contract
 * shared by the entity lifecycle listeners — and models only the delete concern via
 * {@link #onPreDelete(Object, Object, EntityMetadata)},
 * {@link #onPostDelete(Object, Object, EntityMetadata)}, and
 * {@link #onDeleteFailed(Object, Object, EntityMetadata, Throwable)}. To observe other
 * lifecycle operations, implement the corresponding contract, e.g.
 * {@link PersistEventListener} or {@link UpdateEventListener}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code DeleteEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} delete events. A listener whose generic type cannot be
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
public interface DeleteEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is deleted, before the delete
   * statement is built and executed.
   *
   * @param entity the entity to be deleted, or {@code null} if the entity was
   * deleted by id and no instance is available
   * @param id the id of the entity to be deleted, or {@code null} if not available
   * (e.g. when deleting by example)
   * @param metadata the entity metadata; must not be {@code null}
   */
  default void onPreDelete(@Nullable T entity, @Nullable Object id, EntityMetadata metadata) {
  }

  /**
   * Invoked after an entity of the observed type was successfully deleted.
   *
   * @param entity the deleted entity, or {@code null} if the entity was deleted by
   * id and no instance was available
   * @param id the id of the deleted entity, or {@code null} if not available (e.g.
   * when deleting by example)
   * @param metadata the entity metadata; must not be {@code null}
   */
  default void onPostDelete(@Nullable T entity, @Nullable Object id, EntityMetadata metadata) {
  }

  /**
   * Invoked when deleting an entity of the observed type failed.
   *
   * <p>This callback is invoked after any failure raised while deleting the entity,
   * before the exception is propagated to the caller.
   *
   * @param entity the entity that failed to be deleted, or {@code null} if the
   * entity was deleted by id and no instance is available
   * @param id the id of the entity that failed to be deleted, or {@code null} if not
   * available (e.g. when deleting by example)
   * @param metadata the entity metadata; must not be {@code null}
   * @param exception the exception that caused the failure; must not be {@code null}
   */
  default void onDeleteFailed(@Nullable T entity, @Nullable Object id, EntityMetadata metadata, Throwable exception) {
  }

}
