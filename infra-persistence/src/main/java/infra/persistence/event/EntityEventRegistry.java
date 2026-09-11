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
import java.util.List;

import infra.persistence.EntityMetadata;

/**
 * Registry for persistence {@link Listener listeners}: {@link EntityEventListener
 * entity lifecycle listeners} and {@link BatchPersistListener batch persist
 * listeners}.
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.Listener
 * @see infra.persistence.event.EntityEventListener
 * @see infra.persistence.event.BatchPersistListener
 * @see infra.persistence.DefaultEntityManager#getEntityEventRegistry()
 * @since 5.0
 */
public interface EntityEventRegistry {

  /**
   * Register a persistence {@link Listener}. Both {@link EntityEventListener} and
   * {@link BatchPersistListener} instances are accepted.
   *
   * <p>The entity type an {@link EntityEventListener} is interested in is derived
   * from its generic parameter, e.g. {@code new EntityEventListener<ProjectProcess>()
   * {...}} observes only {@code ProjectProcess} entities. A listener whose generic
   * type cannot be resolved observes every entity.
   *
   * @param listener the listener to register; must not be {@code null}
   */
  <T extends Listener> void addListener(T listener);

  /**
   * Register multiple {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to register
   */
  void addListeners(@Nullable Collection<? extends Listener> listeners);

  /**
   * Replace all registered {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to set, or {@code null} to clear all listeners
   */
  void setListeners(@Nullable Collection<? extends Listener> listeners);

  /**
   * Remove a {@link Listener}.
   *
   * @param listener the listener to remove; must not be {@code null}
   */
  void removeListener(Listener listener);

  /**
   * Remove multiple {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to remove; must not be {@code null}
   */
  void removeListeners(Collection<? extends Listener> listeners);

  /**
   * Remove all registered listeners (both entity event and batch persist listeners).
   */
  void clear();

  /**
   * Return the listeners registered under the specified listener contract.
   *
   * <p>The returned list is a live registry-backed view and should be treated as
   * read-only by callers. Listener ordering is the registration order unless the
   * listener contract defines another ordering rule.
   *
   * @param <T> the listener type
   * @param type the listener contract used as the registry key; must not be
   * {@code null}
   * @return the registered listeners, or an empty list if no listeners are
   * registered for the specified type
   */
  <T extends Listener> List<T> getListeners(Class<T> type);

  /**
   * Return the listeners of the specified contract that observe the given entity
   * class, sorted by {@link infra.core.annotation.AnnotationAwareOrderComparator order}.
   *
   * <p>An {@link EntityEventListener} observes an entity class when that class is
   * assignable to the entity type declared by its generic parameter. The result is
   * cached per {@code (listenerType, entityClass)} and invalidated whenever
   * listeners are added or removed, so steady-state dispatch performs no generic
   * type resolution.
   *
   * @param <L> the listener type
   * @param listenerType the listener contract used as the registry key; must not be
   * {@code null}
   * @param entityClass the entity class to match against; must not be {@code null}
   * @return the listeners observing the given entity class, or an empty list if
   * none registered for the specified type observe it
   */
  <L extends Listener> List<L> matchingListeners(Class<L> listenerType, Class<?> entityClass);

}
