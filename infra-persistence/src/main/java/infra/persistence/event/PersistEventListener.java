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
 * Listener for entity <strong>persist</strong> operations.
 *
 * <p>This interface extends {@link EntityEventListener} — the common base contract
 * shared by the entity lifecycle listeners — and models only the persist concern via
 * {@link #onPrePersist(Object, EntityMetadata, PropertyUpdateStrategy)},
 * {@link #onPostPersist(Object, EntityMetadata, PropertyUpdateStrategy)}, and
 * {@link #onPersistFailed(Object, EntityMetadata, PropertyUpdateStrategy, Throwable)}. To
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
 * therefore propagates to the caller.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityEventListener
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface PersistEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked before an entity of the observed type is persisted, before the insert
   * statement is built and executed. Generated identifiers are not yet assigned at
   * this point.
   *
   * <p>The given {@code strategy} is the {@link PropertyUpdateStrategy} that will
   * decide which properties are written back; a modification made to the entity in
   * this callback is only applied when the strategy selects the modified property
   * (with the default {@code noneNull()} strategy, a property is written back once
   * it is non-null).
   *
   * @param entity the entity to be persisted; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy used to select the persisted
   * properties; must not be {@code null}
   */
  default void onPrePersist(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
  }

  /**
   * Invoked after an entity of the observed type was successfully persisted.
   *
   * <p>The entity instance is fully populated by the time this callback is invoked:
   * auto-generated identifiers (if any) have already been written back onto the
   * entity.
   *
   * <p>The given {@code strategy} is the same {@link PropertyUpdateStrategy} that
   * drove the insert. It describes the selection rule, not a snapshot of the
   * written columns: generated identifiers or other entity changes may affect
   * the result of evaluating the strategy again after execution.
   *
   * @param entity the fully populated entity; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy that selected the persisted
   * properties; must not be {@code null}
   */
  default void onPostPersist(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
  }

  /**
   * Invoked when persisting an entity of the observed type failed.
   *
   * <p>This callback is invoked after any failure raised while persisting the
   * entity, before the exception is propagated to the caller. Unlike
   * {@link #onPostPersist}, the entity may only be partially populated (e.g. an
   * auto-generated identifier is not written back when the insert failed).
   *
   * @param entity the entity that failed to be persisted; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   * @param strategy the property update strategy used for the failed persistence;
   * must not be {@code null}
   * @param exception the exception that caused the failure; must not be {@code null}
   */
  default void onPersistFailed(T entity, EntityMetadata metadata, PropertyUpdateStrategy strategy, Throwable exception) {
  }

}
