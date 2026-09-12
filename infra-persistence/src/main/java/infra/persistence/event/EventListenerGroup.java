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
import java.util.Iterator;
import java.util.List;

import infra.util.Assert;

/**
 * A self-managing group of {@link Listener listeners} of a single contract type.
 *
 * <p>The group owns its listener storage: adding, removing, or clearing listeners
 * notifies subclasses through {@link #onListenersChanged()} so derived state (such as
 * the entity-class match cache held by {@link EntityListenerGroup}) can be
 * invalidated. A listener set change is therefore picked up on the next dispatch
 * without any explicit cache maintenance by the caller.
 *
 * <p>This base class only manages listeners of a generic {@link Listener} contract;
 * entity-class aware matching and its cache lives in {@link EntityListenerGroup}.
 *
 * <p>This class is not thread-safe: mutating and dispatching listeners concurrently
 * must be coordinated externally. Iterator-based access is intended for a single
 * owner (typically the {@link EntityEventRegistry} backing this group).
 *
 * @param <T> the listener contract type managed by this group
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityListenerGroup
 * @see infra.persistence.event.EntityEventRegistry
 * @since 5.0
 */
public class EventListenerGroup<T extends Listener> implements Iterable<T> {

  private final ArrayList<T> listeners = new ArrayList<>();

  /**
   * Register a listener with this group. The listener is inserted without any
   * contract-type validation; listeners incompatible with the group's contract type
   * are accepted as long as they can be cast to {@code T} at dispatch time.
   *
   * @param listener the listener to register; must not be {@code null}
   */
  public void addListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.add(listener);
    onListenersChanged();
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
   * Remove the given listener from this group. No-op if the listener is not currently
   * registered.
   *
   * @param listener the listener to remove; must not be {@code null}
   */
  public void removeListener(T listener) {
    Assert.notNull(listener, "Listener is required");
    listeners.remove(listener);
    onListenersChanged();
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
   * Remove all listeners from this group.
   */
  public void clear() {
    listeners.clear();
    onListenersChanged();
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
   * mutate the group so derived state stays valid.
   *
   * @return the registered listeners, in registration order
   */
  public List<T> asList() {
    return listeners;
  }

  /**
   * Return the listeners applicable to the given entity class, as an array ready for
   * allocation-free iteration during dispatch.
   *
   * <p>This base group is not entity aware, so entity-class lookup is not supported
   * and the default implementation always throws {@link UnsupportedOperationException}.
   * The entity-aware subclass {@link EntityListenerGroup} overrides this method to
   * return the listeners observing the given entity class, so a caller holding an
   * {@link EntityEventListener} contract can invoke it uniformly.
   *
   * @param entityClass the entity class to match against; must not be {@code null}
   * @return the applicable listeners, never {@code null}
   * @throws UnsupportedOperationException always, unless overridden by an
   * entity-aware subclass
   */
  public T[] listenersFor(Class<?> entityClass) {
    throw new UnsupportedOperationException(
            "Entity-class lookup is not supported by " + getClass().getSimpleName());
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

  /**
   * Called after the listener set has been mutated so subclasses can invalidate
   * cached derived state. The default implementation does nothing.
   */
  protected void onListenersChanged() {
  }

}