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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.util.Assert;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>Listeners are dispatched by contract type into {@link EventListenerGroup groups}
 * kept in a single {@link Map} keyed by the listener contract type — e.g.
 * {@link EntityEventListener} or {@link BatchPersistListener}. Each group owns its
 * listener storage and per-entity-class match cache, and new listener contract types
 * can be added without changing this registry's storage layout.
 *
 * <p>Dispatch of {@link infra.persistence.event.EntityEvent entity lifecycle events}
 * is performed separately by the entity manager.
 *
 * <p>All mutating methods are intended to be invoked from a single thread during
 * steady-state operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DefaultEntityEventRegistry implements EntityEventRegistry {

  /**
   * Listener groups keyed by their listener contract type.
   */
  private final Map<Class<?>, EventListenerGroup<?>> listenerGroups = new HashMap<>();

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    if (listener instanceof EntityEventListener<?>) {
      groupFor(EntityEventListener.class).addListener(listener);
    }
    if (listener instanceof BatchPersistListener) {
      groupFor(BatchPersistListener.class).addListener(listener);
    }
    if (!(listener instanceof EntityEventListener<?> || listener instanceof BatchPersistListener)) {
      throw new IllegalArgumentException("Unsupported listener type: " + listener.getClass());
    }
  }

  @Override
  public void addListeners(@Nullable Collection<? extends Listener> listeners) {
    if (listeners != null) {
      for (Listener listener : listeners) {
        addListener(listener);
      }
    }
  }

  @Override
  public void setListeners(@Nullable Collection<? extends Listener> listeners) {
    clear();
    addListeners(listeners);
  }

  @Override
  public void removeListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    if (listener instanceof EntityEventListener<?>) {
      EventListenerGroup group = group(EntityEventListener.class);
      if (group != null) {
        group.removeListener(listener);
      }
    }
    if (listener instanceof BatchPersistListener) {
      EventListenerGroup group = group(BatchPersistListener.class);
      if (group != null) {
        group.removeListener(listener);
      }
    }
  }

  @Override
  public void removeListeners(Collection<? extends Listener> listeners) {
    Assert.notNull(listeners, "Listener collection is required");
    for (Listener listener : listeners) {
      removeListener(listener);
    }
  }

  @Override
  public void clear() {
    listenerGroups.clear();
  }

  // ---------------------------------------------------------------------
  // Lookup
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Listener> List<T> getListeners(Class<T> type) {
    EventListenerGroup group = group(type);
    if (group == null) {
      return Collections.emptyList();
    }
    return (List<T>) group.getListeners();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <L extends Listener> List<L> matchingListeners(Class<L> listenerType, Class<?> entityClass) {
    EventListenerGroup group = group(listenerType);
    if (group == null) {
      return Collections.emptyList();
    }
    return (List<L>) group.matchingListeners(entityClass);
  }

  // ---------------------------------------------------------------------
  // Internal
  // ---------------------------------------------------------------------

  private EventListenerGroup group(Class<?> listenerType) {
    return listenerGroups.get(listenerType);
  }

  private EventListenerGroup groupFor(Class<?> listenerType) {
    return listenerGroups.computeIfAbsent(listenerType, type -> new EventListenerGroup<>());
  }

}