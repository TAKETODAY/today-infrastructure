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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import infra.util.Assert;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>Listeners are grouped by contract type into {@link EventListenerGroup groups}
 * kept in a single {@link Map} keyed by the listener contract type. An
 * {@link EntityEventListener} contract gets an entity-class aware
 * {@link EntityListenerGroup} (owning the per-entity-class match cache), while other
 * contracts such as {@link BatchPersistListener} get a plain {@link EventListenerGroup}.
 * New listener contract types can be added without changing this registry's storage
 * layout.
 *
 * <p>Dispatch of entity lifecycle events is performed separately by the entity
 * manager.
 *
 * <p>All mutating methods are intended to be invoked from a single thread during
 * steady-state operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DefaultEntityEventRegistry implements EntityEventRegistry {

  private static final Set<Class<? extends Listener>> supportedListenerTypes = Set.of(
          PersistingEventListener.class,
          UpdatingEventListener.class,
          DeletingEventListener.class,
          PostLoadEventListener.class,
          PostTruncateEventListener.class,
          BatchPersistListener.class);

  /**
   * Listener groups keyed by their listener contract type.
   */
  @SuppressWarnings("rawtypes")
  private final HashMap<Class<?>, EventListenerGroup> listenerGroups = new HashMap<>();

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    boolean unsupported = true;
    for (var listenerType : supportedListenerTypes) {
      if (listenerType.isInstance(listener)) {
        EventListenerGroup listeners = groupFor(listenerType);
        listeners.addListener(listener);
        unsupported = false;
      }
    }

    if (unsupported) {
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
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void removeListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    for (var listenerType : supportedListenerTypes) {
      if (listenerType.isInstance(listener)) {
        EventListenerGroup group = findGroup(listenerType);
        if (group != null) {
          group.removeListener(listener);
        }
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
    for (var group : listenerGroups.values()) {
      group.clear();
    }
    listenerGroups.clear();
  }

  // ---------------------------------------------------------------------
  // Lookup
  // ---------------------------------------------------------------------

  @Override
  public <T extends Listener> EventListenerGroup<T> listeners(Class<T> type) {
    return groupFor(type);
  }

  @SuppressWarnings("unchecked")
  private <T extends Listener> EventListenerGroup<T> findGroup(Class<T> listenerType) {
    return listenerGroups.get(listenerType);
  }

  @SuppressWarnings("unchecked")
  private <T extends Listener> EventListenerGroup<T> groupFor(Class<T> listenerType) {
    return listenerGroups.computeIfAbsent(listenerType, this::createGroup);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private EventListenerGroup createGroup(Class<?> listenerType) {
    if (EntityEventListener.class.isAssignableFrom(listenerType)) {
      return new EntityListenerGroup();
    }
    return new EventListenerGroup();
  }

}