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
import infra.persistence.PropertyUpdateStrategy;

/**
 * Listener for entity <strong>update</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener}, the common base contract
 * shared by all entity lifecycle listeners, and adds the update callbacks
 * {@link #onPreUpdating(Object, EntityMetadata, PropertyUpdateStrategy)} and
 * {@link #onPostUpdating(Object, EntityMetadata)}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code UpdatingEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} update events. A listener whose generic type cannot be
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
public interface UpdatingEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is updated, before the update
   * statement is built and executed.
   *
   * <p>Whether a modification made to the entity in this callback is applied
   * depends on the given {@link PropertyUpdateStrategy}: with the default
   * {@code noneNull()} strategy only non-null properties are written back, and a
   * {@code @Version} property is always overwritten by the framework's own version
   * increment. This callback is therefore best used for observation, validation, or
   * auditing rather than for reliably mutating the entity.
   *
   * @param entity the entity to be updated; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy used to select the updated
   * properties; must not be {@code null}
   */
  default void onPreUpdating(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
  }

  /**
   * Invoked after an entity of the observed type was successfully updated.
   *
   * @param entity the updated entity, reflecting the state before the update was
   * applied; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  default void onPostUpdating(T entity, EntityMetadata metadata) {
  }

}
