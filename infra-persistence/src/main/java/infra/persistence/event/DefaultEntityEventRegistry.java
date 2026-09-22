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



import infra.util.Assert;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>The supported listener contract types are a fixed, small set, so one group is
 * held per contract in a plain array. Each {@link EventListenerGroup} knows the
 * contract type it manages, so registering or removing a listener is a short
 * {@code isInstance} comparison against that type instead of a map lookup. An
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
   * Listener groups, one per supported listener contract type, built eagerly on
   * construction so lookups and iteration never allocate. The contract type each group
   * matches is held by the group itself.
   */
  @SuppressWarnings("rawtypes")
  private final EventListenerGroup[] listenerGroups;

  public DefaultEntityEventRegistry() {
    this.listenerGroups = new EventListenerGroup[] {
            new EntityListenerGroup<>(PostLoadEventListener.class),
            new EntityListenerGroup<>(PersistEventListener.class),
            new EntityListenerGroup<>(UpdateEventListener.class),
            new EntityListenerGroup<>(DeleteEventListener.class),
            new EventListenerGroup<>(BatchPersistListener.class),
            new EventListenerGroup<>(PostTruncateEventListener.class),
    };
  }

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    boolean supported = false;
    for (EventListenerGroup group : listenerGroups) {
      if (group.listenerType().isInstance(listener)) {
        group.addListener(listener);
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
    for (EventListenerGroup group : listenerGroups) {
      if (group.listenerType().isInstance(listener)) {
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
  @SuppressWarnings("rawtypes")
  public void clear() {
    for (EventListenerGroup group : listenerGroups) {
      // Keep the group instance alive so callers holding a reference obtained from
      // listeners(Class) stay valid; only its listeners are removed.
      group.clear();
    }
  }

  // ---------------------------------------------------------------------
  // Lookup
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <T extends Listener> EventListenerGroup<T> listeners(Class<T> type) {
    Assert.notNull(type, "Listener type is required");
    for (EventListenerGroup group : listenerGroups) {
      if (group.listenerType() == type) {
        return (EventListenerGroup<T>) group;
      }
    }
    throw new IllegalArgumentException("Unsupported listener type: " + type);
  }

}
