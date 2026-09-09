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
import infra.util.MultiValueMap;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>All listeners are stored in a single {@link MultiValueMap} keyed by class:
 * {@link BatchPersistListener}s under their contract type, {@link
 * EntityEventListener}s bucketed by the entity type resolved from their generic
 * type parameter at registration time, so dispatch never has to re-resolve
 * generics.
 *
 * <p>Listeners are invoked by {@linkplain EntityEventListener#getOrder() order},
 * lowest value first. All mutating methods as well as the dispatch methods are
 * intended to be invoked from a single thread during steady-state operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DefaultEntityEventRegistry implements EntityEventRegistry {

  /**
   * Single listener storage: keyed either by a listener contract — e.g.
   * {@link BatchPersistListener}, backing {@link #getListeners(Class)} — or by the
   * entity type of an {@link EntityEventListener}, resolved from its generic type
   * parameter at registration time and used for dispatch.
   */
  private final MultiValueMap<Class<?>, Listener> eventListeners =
          MultiValueMap.forSmartListAdaptation();

  private final Map<Class<?>, List<EntityEventListener<?>>> matchingCache = new HashMap<>();

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
    matchingCache.clear();
    if (listener instanceof EntityEventListener<?> entityListener) {
      eventListeners.add(EntityEventListener.class, entityListener);
    }
    if (listener instanceof BatchPersistListener batchPersistListener) {
      eventListeners.add(BatchPersistListener.class, batchPersistListener);
    }
    if (!(listener instanceof EntityEventListener<?> || listener instanceof BatchPersistListener)) {
      throw new IllegalArgumentException("Unsupported listener type: " + listener.getClass());
    }
  }

  @Override
  public void addListeners(Collection<? extends Listener> listeners) {
    Assert.notNull(listeners, "Listener collection is required");
    for (Listener listener : listeners) {
      addListener(listener);
    }
  }

  @Override
  public void setListeners(@Nullable Collection<? extends Listener> listeners) {
    clear();
    if (listeners != null) {
      addListeners(listeners);
    }
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
  @SuppressWarnings("unchecked")
  public <T extends Listener> List<T> getListeners(Class<T> type) {
    List<Listener> listeners = eventListeners.get(type);
    if (listeners == null) {
      return Collections.emptyList();
    }
    return (List<T>) listeners;
  }

  @Override
  public void clear() {
    eventListeners.clear();
    matchingCache.clear();
  }

  // ---------------------------------------------------------------------
  // Dispatch
  // ---------------------------------------------------------------------

  @Override
  public void publishInsert(Object entity, EntityMetadata metadata) {
    EntityInsertEvent<Object> event = new EntityInsertEvent<>(entity, metadata);
    for (EntityEventListener<?> listener : matchingListeners(entity.getClass())) {
      invokeInsert(listener, event);
    }
  }

  @Override
  public void publishUpdate(Object entity, EntityMetadata metadata) {
    EntityUpdateEvent<Object> event = new EntityUpdateEvent<>(entity, metadata);
    for (EntityEventListener<?> listener : matchingListeners(entity.getClass())) {
      invokeUpdate(listener, event);
    }
  }

  @Override
  public void publishDelete(Class<?> entityClass, @Nullable Object entity, @Nullable Object id, EntityMetadata metadata) {
    EntityDeleteEvent<Object> event = new EntityDeleteEvent<>(entityClass, entity, id, metadata);
    for (EntityEventListener<?> listener : matchingListeners(entityClass)) {
      invokeDelete(listener, event);
    }
  }

  /**
   * Return the listeners matching the given entity class
   *
   * <p>The result is cached per entity class and rebuilt lazily after any mutation:
   * dispatch is a single map lookup plus an iteration over the resolved listeners.
   */
  private List<EntityEventListener<?>> matchingListeners(Class<?> entityClass) {
    return matchingCache.computeIfAbsent(entityClass, this::resolveListeners);
  }

  @SuppressWarnings("rawtypes")
  private List<EntityEventListener<?>> resolveListeners(Class<?> entityClass) {
    var listeners = getListeners(EntityEventListener.class);
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

  /**
   * Resolve the entity type declared by the generic parameter of the listener, or
   * {@code null} if it cannot be resolved (in which case the listener observes every
   * entity).
   */
  private static Class<?> resolveEntityType(Listener listener) {
    return ResolvableType.forClass(listener.getClass())
            .getGeneric(0)
            .resolve(Object.class);
  }

  @SuppressWarnings("unchecked")
  private static void invokeInsert(EntityEventListener<?> listener, EntityInsertEvent<Object> event) {
    ((EntityEventListener<Object>) listener).onInsert(event);
  }

  @SuppressWarnings("unchecked")
  private static void invokeUpdate(EntityEventListener<?> listener, EntityUpdateEvent<Object> event) {
    ((EntityEventListener<Object>) listener).onUpdate(event);
  }

  @SuppressWarnings("unchecked")
  private static void invokeDelete(EntityEventListener<?> listener, EntityDeleteEvent<Object> event) {
    ((EntityEventListener<Object>) listener).onDelete(event);
  }

}
