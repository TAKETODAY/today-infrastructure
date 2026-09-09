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
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;

import infra.core.ResolvableType;
import infra.persistence.EntityMetadata;
import infra.util.Assert;
import infra.util.MultiValueMap;

/**
 * Default {@link EntityEventRegistry} implementation.
 *
 * <p>Listeners are stored in a {@link MultiValueMap} keyed by their listener
 * contract. Entity event listeners are additionally filtered by the entity type
 * resolved from their generic type parameter.
 *
 * <p>Listeners are invoked by {@linkplain EntityEventListener#getOrder() order},
 * lowest value first. All mutating methods as well as the dispatch methods are
 * intended to be invoked from a single thread during steady-state operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class DefaultEntityEventRegistry implements EntityEventRegistry {

  private static final Comparator<EntityEventListener<?>> ORDER_COMPARATOR =
          Comparator.comparingInt(EntityEventListener::getOrder);

  private final MultiValueMap<Class<?>, EventListener> eventListeners =
          MultiValueMap.forSmartListAdaptation();

  // ---------------------------------------------------------------------
  // Registration
  // ---------------------------------------------------------------------

  @Override
  public void addListener(Listener listener) {
    Assert.notNull(listener, "Listener is required");
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
    if (listener instanceof EntityEventListener<?>) {
      removeListener(EntityEventListener.class, listener);
    }
    if (listener instanceof BatchPersistListener) {
      removeListener(BatchPersistListener.class, listener);
    }
  }

  private void removeListener(Class<?> listenerType, EventListener listener) {
    List<EventListener> listeners = eventListeners.get(listenerType);
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
  public boolean hasEntityListeners() {
    return !eventListeners.getOrDefault(EntityEventListener.class, List.of()).isEmpty();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends EventListener> List<T> getListeners(Class<T> type) {
    List<EventListener> listeners = eventListeners.get(type);
    if (listeners == null) {
      return Collections.emptyList();
    }
    return (List<T>) listeners;
  }

  @Override
  public void clear() {
    eventListeners.clear();
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
   * Return the listeners matching the given entity class, in {@link #ORDER_COMPARATOR
   * order}.
   *
   * <p>Listeners are bucketed by entity type at registration time; dispatch walks
   * the entity class hierarchy ({@code superclass} chain plus interfaces) and looks
   * up each level in the {@link #entityEventListeners} map directly. Listeners that
   * observe every entity are registered under {@link #ALL_ENTITY_TYPES} and are
   * reached because every class hierarchy eventually ends at {@code Object}.
   */
  private List<EntityEventListener<?>> matchingListeners(Class<?> entityClass) {
    ArrayList<EntityEventListener<?>> matched = new ArrayList<>();
    for (EventListener listener : eventListeners.getOrDefault(EntityEventListener.class, List.of())) {
      EntityEventListener<?> entityListener = (EntityEventListener<?>) listener;
      Class<?> listenerType = resolveEntityType(entityListener);
      if (listenerType == null || listenerType.isAssignableFrom(entityClass)) {
        matched.add(entityListener);
      }
    }
    matched.sort(ORDER_COMPARATOR);
    return matched;
  }

  /**
   * Resolve the entity type declared by the generic parameter of the listener, or
   * {@code null} if it cannot be resolved (in which case the listener observes every
   * entity).
   */
  private static @Nullable Class<?> resolveEntityType(EntityEventListener<?> listener) {
    Class<?> resolved = ResolvableType.forClass(listener.getClass())
            .as(EntityEventListener.class)
            .getGeneric(0)
            .resolve();
    return resolved == null || resolved == Object.class ? null : resolved;
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
