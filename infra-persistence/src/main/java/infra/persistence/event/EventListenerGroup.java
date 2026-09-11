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
 * <p>This class is not thread-safe: mutating and dispatching listeners concurrently
 * must be coordinated externally. Iterator-based access is intended for a single
 * owner (typically the {@link EntityEventRegistry} backing this group).
 *
 * @param <T> the listener contract type managed by this group
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.persistence.event.EntityEventRegistry
 * @see infra.persistence.event.EntityEventListener
 * @see infra.persistence.event.BatchPersistListener
 * @since 5.0
 */
public class EventListenerGroup<T extends Listener> implements Iterable<T> {

  private final ArrayList<T> listeners = new ArrayList<>();

  /**
   * Cached match result per entity class, invalidated on every listener mutation and
   * rebuilt lazily on the next dispatch for that entity class.
   */
  private final HashMap<Class<?>, List<T>> matchingCache = new HashMap<>(); // todo 分层设计

  /**
   * Register a listener with this group. The cached match results are invalidated so
   * the new listener participates in the next dispatch.
   *
   * <p>Note that the listener is inserted without any contract-type or entity-class
   * validation; listeners incompatible with the group's contract type are accepted as
   * long as they can be cast to {@code T} at dispatch time.
   *
   * @param listener the listener to register; must not be {@code null}
   */
  public void addListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.add(listener);
    matchingCache.clear();
  }

  /**
   * Register multiple {@linkplain Listener listeners}, each treated as if passed to
   * {@link #addListener(Listener)} individually.
   *
   * @param listeners the listeners to register; may be {@code null} or empty to do
   * nothing
   */
  public void addListeners(@Nullable Collection<? extends T> listeners) {
    if (listeners != null) {
      for (T listener : listeners) {
        addListener(listener);
      }
    }
  }

  /**
   * Replace all registered {@linkplain Listener listeners} with the given ones.
   * Effectively a {@link #clear()} followed by {@link #addListeners(Collection)}.
   *
   * @param listeners the listeners to set, or {@code null} to clear all listeners
   */
  public void setListeners(@Nullable Collection<? extends T> listeners) {
    clear();
    addListeners(listeners);
  }

  /**
   * Remove the given listener from this group, invalidating the cached match results.
   * No-op if the listener is not currently registered.
   *
   * @param listener the listener to remove; must not be {@code null}
   */
  public void removeListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.remove(listener);
    matchingCache.clear();
  }

  /**
   * Remove multiple {@linkplain Listener listeners}, each treated as if passed to
   * {@link #removeListener(T)} individually.
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
   * Remove all listeners from this group and clear the cached match results.
   */
  public void clear() {
    listeners.clear();
    matchingCache.clear();
  }

  /**
   * Return whether this group holds no listener.
   *
   * @return {@code true} if no listener is registered, {@code false} otherwise
   */
  public boolean isEmpty() {
    return listeners.isEmpty();
  }

  /**
   * Return the number of listeners registered in this group.
   *
   * @return the number of registered listeners
   */
  public int size() {
    return listeners.size();
  }

  /**
   * Return the live listener list backing this group. It should be treated as
   * read-only by callers; use {@link #addListener} and {@link #removeListener} to
   * mutate the group so the cached match results stay valid.
   *
   * @return the registered listeners, in registration order
   */
  public List<T> asList() {
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
   * <p>The returned list is an internal, unmodifiable-by-convention snapshot of the
   * match result: it is safe to read after further mutations, but callers must not
   * modify it.
   *
   * @param entityClass the entity class to match against; must not be {@code null}
   * @return the matching listeners, or an empty list if no listener observes the
   * entity class; never {@code null}
   */
  public List<T> entityListeners(Class<?> entityClass) {
    Assert.notNull(entityClass, "Entity class is required");
    if (listeners.isEmpty()) {
      return Collections.emptyList();
    }
    return matchingCache.computeIfAbsent(entityClass, this::resolveListeners);
  }

  /**
   * Return an iterator over the registered listeners, in registration order.
   *
   * @return an iterator over the registered listeners
   */
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

  private static Class<?> resolveEntityType(Listener listener) {
    return ResolvableType.forClass(listener.getClass())
            .as(EntityEventListener.class)
            .getGeneric(0)
            .resolve(Object.class);
  }

}