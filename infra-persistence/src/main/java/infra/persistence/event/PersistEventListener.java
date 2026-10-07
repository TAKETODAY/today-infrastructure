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

import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyUpdateStrategy;

/**
 * Listener for entity <strong>persist</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener} — the common base contract
 * shared by the entity lifecycle listeners — and models only the persist concern via
 * {@link #onPrePersist(Object, EntityMetadata, PropertyUpdateStrategy)},
 * {@link #onPostPersist(Object, EntityMetadata, List)}, and
 * {@link #onPersistFailed(Object, EntityFailureContext)}. To
 * observe other lifecycle operations, implement the corresponding contract, e.g.
 * {@link UpdateEventListener} or {@link DeleteEventListener}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code PersistEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} persist events. A listener whose generic type cannot be
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
public interface PersistEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is inserted and before
   * application-generated identifiers are assigned.
   *
   * <p>The primary key may not yet be available in this callback. Application ID
   * generation runs after all pre-persist callbacks and only when the ID property
   * is still {@code null}. This callback may therefore populate fields required
   * by an {@link infra.persistence.IdGenerator} or assign an application-generated
   * ID itself; an existing non-null ID is preserved without invoking the generator.
   * Database-generated identifiers are read back only after the INSERT executes.
   *
   * <p>Listeners run in registration order. The returned strategy is passed to
   * the next listener, and the final strategy is evaluated after all callbacks
   * and ID generation. Return the supplied strategy when only modifying or
   * validating the entity. Value-dependent rules should be evaluated lazily.
   *
   * <p>The effective {@code strategy} is the {@link PropertyUpdateStrategy} that will
   * decide which ordinary properties are written back; a modification made to the entity in
   * this callback is only applied when the strategy selects the modified property
   * (with the default {@code noneNull()} strategy, a property is written back once
   * it is non-null). Application-generated primary keys are included in the INSERT
   * independently of this strategy.
   *
   * @param entity the entity to be persisted; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy used to select the persisted
   * properties; must not be {@code null}
   * @return the effective selection strategy, never {@code null}
   */
  default PropertyUpdateStrategy onPrePersist(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    return strategy;
  }

  /**
   * Invoked after an entity of the observed type was successfully persisted.
   *
   * <p>The entity instance is fully populated by the time this callback is invoked:
   * auto-generated identifiers (if any) have already been written back onto the
   * entity.
   *
   * <p>The given properties are the actual INSERT columns in SQL parameter order,
   * exposed as an immutable list. Database-generated IDs are not included, even
   * though their values have been written back to the entity. This callback does
   * not imply that the surrounding transaction has committed.
   *
   * @param entity the fully populated entity; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param properties the immutable list of inserted properties in SQL parameter order
   */
  default void onPostPersist(T entity, EntityMetadata metadata, List<EntityProperty> properties) {
  }

  /**
   * Invoked when persisting an entity of the observed type failed.
   *
   * <p>This callback is invoked after any failure raised while persisting the
   * entity, before the exception is propagated to the caller. Unlike
   * {@link #onPostPersist}, the entity may only be partially populated (e.g. an
   * auto-generated identifier is not written back when the insert failed).
   * Exceptions thrown by failure listeners are suppressed on the original failure.
   *
   * @param entity the entity that failed to be persisted; must not be {@code null}
   * @param context the failure snapshot, including phase and original exception;
   * does not imply rollback or absence of database changes
   */
  default void onPersistFailed(T entity, EntityFailureContext context) {
  }

}
