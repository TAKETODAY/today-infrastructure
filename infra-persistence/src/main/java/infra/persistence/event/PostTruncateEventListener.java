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

import infra.persistence.EntityManager;
import infra.persistence.EntityMetadata;

/**
 * Listener for {@linkplain EntityManager#truncate(Class) truncate} operations.
 *
 * <p>This interface extends {@link EntityEventListener}, so an implementation
 * observes the full entity lifecycle (persist / update / delete / load) through the
 * inherited callbacks in addition to reacting upon truncation via
 * {@link #onPostTruncate(Class, EntityMetadata)}.
 *
 * <p>The entity type this listener observes is declared by its generic type
 * parameter, e.g. {@code PostTruncateEventListener<ProjectProcess>} receives only
 * {@code ProjectProcess} truncate events. A listener whose generic type cannot be
 * resolved observes every entity.
 *
 * <p>Truncation operates on the whole table, so there is no entity instance to
 * carry on the event: the callback receives the truncated {@linkplain
 * #onPostTruncate(Class, EntityMetadata) entity class} together with its metadata.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link EntityManager} right after the table was truncated; an
 * exception thrown by a listener therefore propagates to the caller.
 *
 * @param <T> the entity type to observe
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityEventListener
 * @see EntityManager#truncate(Class)
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface PostTruncateEventListener<T> extends EntityEventListener<T> {

  /**
   * Invoked after the table of the observed entity type was truncated.
   *
   * <p>No entity instance is available; the callback exposes the entity class that
   * was truncated and its metadata, e.g. to react upon {@link EntityMetadata#tableName}.
   *
   * @param entityClass the truncated entity class; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  void onPostTruncate(Class<?> entityClass, EntityMetadata metadata);

}
