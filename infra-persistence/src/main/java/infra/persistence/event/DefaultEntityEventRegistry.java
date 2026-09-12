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

import infra.util.Assert;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>The supported listener contract types are a fixed, small set, so groups are held
 * in a plain array indexed in lockstep with that set. Looking up a group is therefore
 * a short identity comparison over the contract types instead of a map lookup, and an
 * {@link EntityEventListener} contract gets an entity-class aware
 * {@link EntityListenerGroup} (owning the per-entity-class match cache) while other
 * contracts such as {@link BatchPersistListener} get a plain {@link EventListenerGroup}.
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

  /**
   * The listener contract types supported by this registry, in a fixed order that is
   * shared with {@link #listenerGroups} by index.
   */
  private static final Class<?>[] supportedListenerTypes = {
          PostLoadEventListener.class,
          PersistingEventListener.class,
          UpdatingEventListener.class,
          DeletingEventListener.class,
          BatchPersistListener.class,
          PostTruncateEventListener.class,
  };

  /**
   * Listener groups indexed in lockstep with {@link #supportedListenerTypes}; a
   * {@code null} slot is created lazily on first use.
   */
  @SuppressWarnings("rawtypes")
  private final @Nullable EventListenerGroup[] listenerGroups = new EventListenerGroup[supportedListenerTypes.length];

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings({ "unchecked" })
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    boolean supported = false;
    for (int i = 0; i < supportedListenerTypes.length; i++) {
      if (supportedListenerTypes[i].isInstance(listener)) {
        groupAt(i).addListener(listener);
        supported = true;
      }
    }

    if (!supported) {
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
    for (int i = 0; i < supportedListenerTypes.length; i++) {
      if (supportedListenerTypes[i].isInstance(listener)) {
        EventListenerGroup group = listenerGroups[i];
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
  @SuppressWarnings("rawtypes")
  public void clear() {
    for (int i = 0; i < listenerGroups.length; i++) {
      EventListenerGroup group = listenerGroups[i];
      if (group != null) {
        group.clear();
        listenerGroups[i] = null;
      }
    }
  }

  // ---------------------------------------------------------------------
  // Lookup
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Listener> EventListenerGroup<T> listeners(Class<T> type) {
    Assert.notNull(type, "Listener type is required");
    for (int i = 0; i < supportedListenerTypes.length; i++) {
      if (supportedListenerTypes[i] == type) {
        return groupAt(i);
      }
    }
    throw new IllegalArgumentException("Unsupported listener type: " + type);
  }

  @SuppressWarnings("rawtypes")
  private EventListenerGroup groupAt(int index) {
    EventListenerGroup group = listenerGroups[index];
    if (group == null) {
      group = createGroup(supportedListenerTypes[index]);
      listenerGroups[index] = group;
    }
    return group;
  }

  @SuppressWarnings("rawtypes")
  private EventListenerGroup createGroup(Class<?> listenerType) {
    if (EntityEventListener.class.isAssignableFrom(listenerType)) {
      return new EntityListenerGroup<>(listenerType);
    }
    return new EventListenerGroup();
  }

}
