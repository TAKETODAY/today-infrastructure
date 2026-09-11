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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.util.Assert;

/**
 * A self-managing group of {@link Listener listeners} of a single contract type.
 *
 * <p>The group owns its listener storage and the per-entity-class match results:
 * adding, removing, or clearing listeners automatically invalidates the cached match
 * results, so a listener set change is picked up on the next dispatch without any
 * explicit cache maintenance by the caller.
 *
 * <p>For {@link EntityEventListener}s the observed entity class is derived from the
 * generic type parameter; a listener whose generic type cannot be resolved, as well
 * as listeners of other contract types, observe every entity. Matched listeners are
 * sorted by {@link AnnotationAwareOrderComparator order}.
 *
 * @param <T> the listener contract type managed by this group
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityEventRegistry
 * @see infra.persistence.event.EntityEventListener
 * @since 5.0
 */
public class EventListenerGroup<T extends Listener> implements Iterable<T> {

  private final List<T> listeners = new ArrayList<>();

  /**
   * Cached match result per entity class, invalidated on every listener mutation and
   * rebuilt lazily on the next dispatch for that entity class.
   */
  private final Map<Class<?>, List<T>> matchingCache = new HashMap<>();

  /**
   * Register a listener with this group.
   *
   * @param listener the listener to register; must not be {@code null}
   */
  public void addListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.add(listener);
    matchingCache.clear();
  }

  /**
   * Register multiple {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to register
   */
  public void addListeners(@Nullable Collection<? extends T> listeners) {
    if (listeners != null) {
      for (T listener : listeners) {
        addListener(listener);
      }
    }
  }

  /**
   * Replace all registered {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to set, or {@code null} to clear all listeners
   */
  public void setListeners(@Nullable Collection<? extends T> listeners) {
    clear();
    addListeners(listeners);
  }

  /**
   * Remove the given listener from this group.
   *
   * @param listener the listener to remove; must not be {@code null}
   */
  public void removeListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.remove(listener);
    matchingCache.clear();
  }

  /**
   * Remove multiple {@linkplain Listener listeners}.
   *
   * @param listeners the listeners to remove; must not be {@code null}
   */
  public void removeListeners(Collection<? extends T> listeners) {
    Assert.notNull(listeners, "Listener collection is required");
    for (T listener : listeners) {
      removeListener(listener);
    }
  }

  /**
   * Remove all listeners from this group.
   */
  public void clear() {
    listeners.clear();
    matchingCache.clear();
  }

  /**
   * Return whether this group holds no listener.
   *
   * @return whether this group holds no listener
   */
  public boolean isEmpty() {
    return listeners.isEmpty();
  }

  /**
   * Return the number of listeners in this group.
   */
  public int size() {
    return listeners.size();
  }

  /**
   * Return the live listener list backing this group. It should be treated as
   * read-only by callers; use {@link #addListener} and {@link #removeListener} to
   * mutate the group so the cached match results stay valid.
   *
   * @return the registered listeners
   */
  public List<T> getListeners() {
    return listeners;
  }

  /**
   * Return the listeners observing the given entity class, sorted by
   * {@link AnnotationAwareOrderComparator order}.
   *
   * <p>The result is cached per entity class and rebuilt lazily after any mutation,
   * so steady-state dispatch is a single map lookup plus an iteration over the
   * resolved listeners. The returned list can be iterated directly.
   *
   * @param entityClass the entity class to match against; must not be {@code null}
   * @return the matching listeners, or an empty list if no listener observes the
   * entity class
   */
  public List<T> matchingListeners(Class<?> entityClass) {
    Assert.notNull(entityClass, "Entity class is required");
    if (listeners.isEmpty()) {
      return Collections.emptyList();
    }
    return matchingCache.computeIfAbsent(entityClass, this::resolveListeners);
  }

  @Override
  public Iterator<T> iterator() {
    return listeners.iterator();
  }

  private List<T> resolveListeners(Class<?> entityClass) {
    ArrayList<T> matched = new ArrayList<>(listeners.size());
    for (T listener : listeners) {
      if (resolveEntityType(listener).isAssignableFrom(entityClass)) {
        matched.add(listener);
      }
    }
    matched.trimToSize();
    AnnotationAwareOrderComparator.sort(matched);
    return matched;
  }

  /**
   * Resolve the entity type declared by the generic parameter of an
   * {@link EntityEventListener}, or {@code Object} if it cannot be resolved (in
   * which case the listener observes every entity) or the listener is not an
   * {@link EntityEventListener}.
   */
  private static Class<?> resolveEntityType(Listener listener) {
    if (listener instanceof EntityEventListener<?>) {
      return ResolvableType.forClass(listener.getClass())
              .as(EntityEventListener.class)
              .getGeneric(0)
              .resolve(Object.class);
    }
    return Object.class;
  }

}