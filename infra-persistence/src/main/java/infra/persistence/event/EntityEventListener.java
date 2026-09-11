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
 * by the {@link infra.persistence.EntityManager} in
 * {@linkplain infra.core.annotation.AnnotationAwareOrderComparator order} right
 * after the underlying statement completed; an exception thrown by a listener
 * therefore propagates to the caller.
 *
 * <p><strong>Usage example</strong> — react only on the deletion of
 * {@code ProjectProcess} entities, no more sprawling {@code instanceof} dispatch:
 * <pre>{@code
 * entityManager.getEntityEventRegistry()
 *     .addListener(new EntityEventListener<ProjectProcess>() {
 *       @Override
 *       public void afterDelete(EntityDeleteEvent<ProjectProcess> event) {
 *         ProjectProcess projectProcess = event.getEntity();   // type-safe, may be null
 *         // handle logic...
 *       }
 *     });
 * }</pre>
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityEventRegistry
 * @see infra.persistence.event.EntityPersistEvent
 * @see infra.persistence.event.EntityUpdateEvent
 * @see infra.persistence.event.EntityDeleteEvent
 * @see infra.core.annotation.Order
 * @see infra.core.Ordered
 * @since 5.0
 */
public interface EntityEventListener<T> extends Listener {

  /**
   * Invoked before an entity of the observed type is deleted, before the delete
   * statement is built and executed.
   *
   * @param event the delete event
   */
  default void onPreDelete(EntityDeleteEvent<T> event) {
  }

  /**
   * Invoked before an entity of the observed type is updated, before the update
   * statement is built and executed. Listeners may modify the entity; the changes
   * are picked up by the update operation.
   *
   * @param event the update event holding the entity to be updated
   */
  default void onPreUpdate(EntityUpdateEvent<T> event) {
  }

  /**
   * Invoked after an entity of the observed type was successfully updated.
   *
   * @param event the update event holding the entity state before the update
   */
  default void onPostUpdate(EntityUpdateEvent<T> event) {
  }

  /**
   * Invoked after an entity of the observed type was successfully deleted.
   *
   * @param event the delete event
   */
  default void onPostDelete(EntityDeleteEvent<T> event) {
  }

}