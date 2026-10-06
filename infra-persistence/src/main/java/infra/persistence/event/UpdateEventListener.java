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

import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyUpdateStrategy;

/**
 * Listener for entity <strong>update</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener} — the common base contract
 * shared by the entity lifecycle listeners — and models only the update concern via
 * {@link #onPreUpdate(Object, EntityMetadata, PropertyUpdateStrategy)},
 * {@link #onPostUpdate(Object, EntityMetadata, List, int)}, and
 * {@link #onUpdateFailed(Object, EntityMetadata, List, Throwable)}. To
 * observe other lifecycle operations, implement the corresponding contract, e.g.
 * {@link PersistEventListener} or {@link DeleteEventListener}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code UpdateEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} update events. A listener whose generic type cannot be
 * resolved observes every entity.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link infra.persistence.EntityManager}; an exception thrown by a listener
 * therefore propagates to the caller, except that failure callback exceptions
 * are suppressed on the original failure.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityEventListener
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface UpdateEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is updated, before the update
   * statement is built and executed.
   *
   * <p>Whether a modification made to the entity in this callback is applied
   * depends on the effective {@link PropertyUpdateStrategy}: with the default
   * {@code noneNull()} strategy only non-null properties are written back, and a
   * {@code @Version} property is always overwritten by the framework's own version
   * increment.
   *
   * <p>Listeners run in registration order, passing the returned strategy to
   * the next listener. The final strategy is evaluated after all callbacks.
   * Return the supplied strategy when only modifying or validating the entity.
   * Value-dependent rules should be evaluated lazily. Version handling and
   * condition properties retain their framework semantics.
   *
   * @param entity the entity to be updated; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy used to select the updated
   * properties; must not be {@code null}
   * @return the effective selection strategy, never {@code null}
   */
  default PropertyUpdateStrategy onPreUpdate(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    return strategy;
  }

  /**
   * Invoked after an entity of the observed type was successfully updated.
   *
   * <p>The properties are the actual SET columns in SQL parameter order, exposed
   * as an immutable list. They include the updated version property, but exclude
   * properties used only in WHERE conditions. The list is available even when
   * no rows matched. This callback does not imply transaction commit.
   *
   * @param entity the current entity, including audit values and the incremented
   * version when applicable; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param properties the immutable list of SET properties in SQL parameter order
   * @param affectedRows the affected row count, or JDBC SUCCESS_NO_INFO for a batch
   */
  default void onPostUpdate(T entity, EntityMetadata metadata, List<EntityProperty> properties, int affectedRows) {
  }

  /**
   * Invoked when updating an entity of the observed type failed.
   *
   * <p>This callback is invoked after any failure raised while updating the entity,
   * before the exception is propagated to the caller. Besides database errors this
   * also covers an optimistic locking failure, in which case the entity already
   * carries the incremented version.
   * Exceptions thrown by failure listeners are suppressed on the original failure.
   *
   * @param entity the entity that failed to be updated; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param properties the immutable selected SET properties in SQL parameter order,
   * or {@code null} if property selection did not complete; never a partial list
   * @param exception the exception that caused the failure; must not be {@code null}
   */
  default void onUpdateFailed(T entity, EntityMetadata metadata, @Nullable List<EntityProperty> properties, Throwable exception) {
  }

}
