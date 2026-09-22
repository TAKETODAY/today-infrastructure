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
 * <p>Truncation operates on a whole table, so there is no entity instance to carry
 * on the callback and no entity type to observe: the callback receives the truncated
 * entity class together with its metadata. As a consequence this contract does not
 * extend {@link EntityEventListener} — it is invoked for every truncated table and
 * has no generic entity type to resolve.
 *
 * <p>Listeners are invoked <strong>synchronously</strong> by the
 * {@link EntityManager} right after the table was truncated; an exception thrown by
 * a listener therefore propagates to the caller.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityManager#truncate(Class)
 * @see EntityEventRegistry
 * @since 5.0
 */
public interface PostTruncateEventListener extends Listener {

  /**
   * Invoked after the table of the given entity class was truncated.
   *
   * <p>No entity instance is available; the callback exposes the truncated entity
   * class and its metadata, e.g. to react upon {@link EntityMetadata#getTableName()}.
   *
   * @param entityClass the truncated entity class; must not be {@code null}
   * @param metadata the entity metadata; must not be {@code null}
   */
  void onPostTruncate(Class<?> entityClass, EntityMetadata metadata);

}