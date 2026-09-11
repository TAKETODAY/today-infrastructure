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
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.persistence.EntityMetadata;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>All listeners are stored in a single {@link MultiValueMap} keyed by their
 * listener contract type — {@link EntityEventListener} or
 * {@link BatchPersistListener}. Dispatch resolves the entity type of every entity
 * event listener once, cached per entity class, so steady-state dispatch is a map
 * lookup followed by an iteration over the resolved listeners.
 *
 * <p>Listeners are invoked in {@linkplain AnnotationAwareOrderComparator order},
 * lowest value first. All mutating methods as well as the dispatch methods are
 * intended to be invoked from a single thread during steady-state operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DefaultEntityEventRegistry implements EntityEventRegistry {

  /**
   * Single listener storage keyed by listener contract type — e.g.
   * {@link BatchPersistListener} — backing {@link #getListeners(Class)}.
   */
  private final MultiValueMap<Class<?>, Listener> eventListeners =
          MultiValueMap.forSmartListAdaptation();

  /**
   * Cached order-sorted listeners for each encountered entity class, cleared on
   * every mutation and rebuilt lazily on first dispatch for that entity class.
   */
  private final Map<Class<?>, List<EntityEventListener<?>>> matchingCache = new HashMap<>();

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    matchingCache.clear();
    if (listener instanceof EntityEventListener<?>) {
      eventListeners.add(EntityEventListener.class, listener);
    }
    if (listener instanceof BatchPersistListener) {
      eventListeners.add(BatchPersistListener.class, listener);
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
    matchingCache.clear();
    if (listener instanceof EntityEventListener<?>) {
      removeListener(EntityEventListener.class, listener);
    }
    if (listener instanceof BatchPersistListener) {
      removeListener(BatchPersistListener.class, listener);
    }
  }

  private void removeListener(Class<?> listenerType, Listener listener) {
    List<Listener> listeners = eventListeners.get(listenerType);
    if (listeners != null) {
      listeners.remove(listener);
      if (listeners.isEmpty()) {
        eventListeners.remove(listenerType);
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
  public <T extends Listener> List<T> getListeners(Class<T> type) {
    List<T> listeners = listeners(type);
    if (listeners == null) {
      return Collections.emptyList();
    }
    return listeners;
  }

  @Override
  public void clear() {
    eventListeners.clear();
    matchingCache.clear();
  }

  @SuppressWarnings("unchecked")
  private <T extends Listener> @Nullable List<T> listeners(Class<T> type) {
    return (List<T>) eventListeners.get(type);
  }

  // ---------------------------------------------------------------------
  // Dispatch
  // ---------------------------------------------------------------------

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void publishPersist(Object entity, EntityMetadata metadata) {
    EntityPersistEvent<Object> event = new EntityPersistEvent<>(entity, metadata);
    for (EntityEventListener listener : matchingListeners(entity.getClass())) {
      listener.onPersist(event);
    }
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void publishUpdate(Object entity, EntityMetadata metadata) {
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (EntityEventListener listener : matchingListeners(entity.getClass())) {
      listener.onUpdate(event);
    }
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void publishDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (EntityEventListener listener : matchingListeners(entityClass)) {
      listener.onDelete(event);
    }
  }

  /**
   * Return the listeners matching the given entity class, sorted by
   * {@link AnnotationAwareOrderComparator order}.
   *
   * <p>The result is cached per entity class and rebuilt lazily after any mutation:
   * dispatch is a single map lookup plus an iteration over the resolved listeners.
   */
  private List<EntityEventListener<?>> matchingListeners(Class<?> entityClass) {
    return matchingCache.computeIfAbsent(entityClass, this::resolveListeners);
  }

  @SuppressWarnings("rawtypes")
  private List<EntityEventListener<?>> resolveListeners(Class<?> entityClass) {
    var listeners = listeners(EntityEventListener.class);
    if (CollectionUtils.isNotEmpty(listeners)) {
      ArrayList<EntityEventListener<?>> matched = new ArrayList<>(listeners.size());
      for (EntityEventListener listener : listeners) {
        Class<?> entityType = resolveEntityType(listener);
        if (entityType.isAssignableFrom(entityClass)) {
          matched.add(listener);
        }
      }
      matched.trimToSize();
      AnnotationAwareOrderComparator.sort(matched);
      return matched;
    }
    return Collections.emptyList();
  }

  /**
   * Resolve the entity type declared by the generic parameter of the listener, or
   * {@link Object} if it cannot be resolved (in which case the listener observes
   * every entity).
   */
  private static Class<?> resolveEntityType(Listener listener) {
    return ResolvableType.forClass(listener.getClass())
            .getGeneric(0)
            .resolve(Object.class);
  }

}
