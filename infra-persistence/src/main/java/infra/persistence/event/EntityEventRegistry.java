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

import java.util.Collection;

/**
 * Registry for persistence {@link Listener listeners}.
 *
 * <p>Listeners are registered <strong>generically</strong> via {@link #addListener}
 * without any binding to a concrete entity class: the entity type an
 * {@link EntityEventListener} wants to observe is derived from its generic type
 * parameter. Dispatch of {@link infra.persistence.event.EntityEvent entity lifecycle
 * events} is performed by the {@link infra.persistence.EntityManager} against the
 * registered listeners.
 *
 * <p>This interface is deliberately decoupled from any IoC container or application
 * event mechanism: it is just a plain registration registry and can be used in a
 * bare JDBC environment.
 *
 * <p>All {@code Listener} instances registered here are grouped by their contract
 * type; {@link #listeners(Class)} exposes the {@link EventListenerGroup} managing a
 * single contract type, e.g. to iterate over all {@link EntityEventListener}s or to
 * dispatch an event directly.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.Listener
 * @see infra.persistence.event.EntityEventListener
 * @see infra.persistence.event.BatchPersistListener
 * @see infra.persistence.event.EventListenerGroup
 * @see infra.persistence.DefaultEntityManager#getEntityEventRegistry()
 * @since 5.0
 */
public interface EntityEventRegistry {

  /**
   * Register a persistence {@link Listener}. Both {@link EntityEventListener} and
   * {@link BatchPersistListener} instances are accepted and grouped by their
   * contract type for dispatch.
   *
   * <p>The entity type an {@link EntityEventListener} is interested in is derived
   * from its generic parameter, e.g. {@code new EntityEventListener<ProjectProcess>()
   * {...}} observes only {@code ProjectProcess} entities. A listener whose generic
   * type cannot be resolved observes every entity.
   *
   * <p>Registering the same listener instance twice has no effect; the listener is
   * stored by identity and delivered at most once per dispatch.
   *
   * @param listener the listener to register; must not be {@code null}
   */
  <T extends Listener> void addListener(T listener);

  /**
   * Register multiple {@linkplain Listener listeners}, each treated as if passed to
   * {@link #addListener(Listener)} individually.
   *
   * @param listeners the listeners to register; may be {@code null} or empty to do
   * nothing
   */
  void addListeners(@Nullable Collection<? extends Listener> listeners);

  /**
   * Replace all registered {@linkplain Listener listeners} with the given ones.
   * Effectively a {@link #clear()} followed by {@link #addListeners(Collection)}.
   *
   * @param listeners the listeners to set, or {@code null} to clear all listeners
   */
  void setListeners(@Nullable Collection<? extends Listener> listeners);

  /**
   * Remove a {@link Listener}. No-op if the listener is not currently registered.
   *
   * @param listener the listener to remove; must not be {@code null}
   */
  void removeListener(Listener listener);

  /**
   * Remove multiple {@linkplain Listener listeners}, each treated as if passed to
   * {@link #removeListener(Listener)} individually.
   *
   * @param listeners the listeners to remove; must not be {@code null}
   */
  void removeListeners(Collection<? extends Listener> listeners);

  /**
   * Remove all registered listeners (both entity event and batch persist listeners).
   */
  void clear();

  /**
   * Return the {@link EventListenerGroup} managing the listeners of the given
   * contract type, creating it on first access.
   *
   * <p>The group exposes the registered listeners (e.g. via
   * {@link EventListenerGroup#iterator()} or {@link EventListenerGroup#asList()}) and
   * allows a caller to dispatch events directly against the matching listeners. Note
   * that the {@link infra.persistence.EntityManager} dispatches events automatically
   * during persist / update / delete, so explicit dispatch is only needed for custom
   * listeners or event flows.
   *
   * @param type the listener contract type, e.g. {@code EntityEventListener.class}
   * or {@code BatchPersistListener.class}; must not be {@code null}
   * @param <T> the listener contract type
   * @return the group managing the listeners of the given type, never {@code null}
   */
  <T extends Listener> EventListenerGroup<T> listeners(Class<T> type);

}
