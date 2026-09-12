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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import infra.core.ResolvableType;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.util.Assert;

/**
 * A {@link EventListenerGroup} specialized for {@link EntityEventListener entity
 * lifecycle listeners}.
 *
 * <p>Besides the plain listener storage of the base class, this group maintains the
 * cached match results used to dispatch an entity event only to the listeners
 * observing the affected entity class. Adding, removing, or clearing listeners
 * invalidates the cache, which is rebuilt lazily on the next dispatch for that entity
 * class.
 *
 * <p>The entity type a listener observes is derived from the generic type parameter of
 * its {@link EntityEventListener} super-interface — e.g.
 * {@code UpdatingEventListener<ProjectProcess>} observes {@code ProjectProcess}. A
 * listener whose generic type cannot be resolved observes every entity. Matched
 * listeners are sorted by {@link AnnotationAwareOrderComparator order}.
 *
 * @param <T> the entity listener contract type managed by this group
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EventListenerGroup
 * @see EntityEventListener
 * @since 5.0
 */
public class EntityListenerGroup<T extends EntityEventListener<?>> extends EventListenerGroup<T> {

  private final HashMap<Class<?>, List<T>> matchingCache = new HashMap<>();

  @Override
  protected void onListenersChanged() {
    matchingCache.clear();
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
   * entity class; never {@code null}
   */
  @Override
  public List<T> matchingListeners(Class<?> entityClass) {
    Assert.notNull(entityClass, "Entity class is required");
    if (isEmpty()) {
      return Collections.emptyList();
    }
    return matchingCache.computeIfAbsent(entityClass, this::resolveListeners);
  }

  private List<T> resolveListeners(Class<?> entityClass) {
    ArrayList<T> matched = new ArrayList<>(size());
    for (T listener : this) {
      if (resolveEntityType(listener).isAssignableFrom(entityClass)) {
        matched.add(listener);
      }
    }
    matched.trimToSize();
    AnnotationAwareOrderComparator.sort(matched);
    return matched;
  }

  private static Class<?> resolveEntityType(EntityEventListener<?> listener) {
    return ResolvableType.forClass(listener.getClass())
            .as(EntityEventListener.class)
            .getGeneric(0)
            .resolve(Object.class);
  }

}