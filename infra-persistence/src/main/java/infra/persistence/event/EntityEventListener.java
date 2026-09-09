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

import infra.core.Ordered;

/**
 * Listener for {@link EntityEvent entity lifecycle events}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code EntityEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} events. A listener whose generic type cannot be resolved
 * observes every entity.
 *
 * <p>All methods are {@code default} no-ops so an implementation only has to override
 * the operations it is interested in. Listeners are invoked <strong>synchronously</strong>
 * by the {@link infra.persistence.EntityManager} in {@linkplain #getOrder() order}
 * right after the underlying statement completed; an exception thrown by a listener
 * therefore propagates to the caller.
 *
 * <p><strong>Usage example</strong> — react only on the deletion of
 * {@code ProjectProcess} entities, no more sprawling {@code instanceof} dispatch:
 * <pre>{@code
 * entityManager.getEntityEventRegistry()
 *     .addEntityListener(new EntityEventListener<ProjectProcess>() {
 *       @Override
 *       public void onDelete(EntityDeleteEvent<ProjectProcess> event) {
 *         ProjectProcess projectProcess = event.getEntity();   // type-safe, may be null
 *         // handle logic...
 *       }
 *     });
 * }</pre>
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityEventRegistry
 * @see infra.persistence.event.EntityInsertEvent
 * @see infra.persistence.event.EntityUpdateEvent
 * @see infra.persistence.event.EntityDeleteEvent
 * @since 5.0
 */
public interface EntityEventListener<T> extends Listener {

  /**
   * Return the order in which this listener is invoked. Lower values have higher
   * priority (i.e. invoked first).
   */
  default int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * Invoked after an entity of the observed type was successfully persisted.
   *
   * @param event the insert event holding the fully populated entity
   */
  default void onInsert(EntityInsertEvent<T> event) {
  }

  /**
   * Invoked after an entity of the observed type was successfully updated.
   *
   * @param event the update event holding the entity state before the update
   */
  default void onUpdate(EntityUpdateEvent<T> event) {
  }

  /**
   * Invoked after an entity of the observed type was successfully deleted.
   *
   * @param event the delete event
   */
  default void onDelete(EntityDeleteEvent<T> event) {
  }

}