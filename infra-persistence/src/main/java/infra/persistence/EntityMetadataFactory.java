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

package infra.persistence;

import infra.util.MapCache;

/**
 * Factory for creating and caching {@link EntityMetadata} by entity class.
 *
 * <p>The cache is scoped to each factory instance, allowing different factories
 * to use different mapping configurations for the same entity class. Metadata
 * is reused across persistence operations through {@link #getEntityMetadata(Class)}.
 *
 * <p>Subclasses implement {@link #createEntityMetadata(Class)} to define the mapping
 * strategy. Configure the factory before retrieving cached metadata: subsequent
 * configuration changes do not invalidate existing cache entries.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityMetadata
 * @see DefaultEntityMetadataFactory
 * @see IllegalEntityException
 * @since 4.0 2022/8/16 23:28
 */
public abstract class EntityMetadataFactory {

  final MapCache<Class<?>, EntityMetadata, EntityMetadataFactory> entityCache = new MapCache<>() {

    @Override
    protected EntityMetadata createValue(Class<?> entityClass, EntityMetadataFactory factory) {
      return factory.createEntityMetadata(entityClass);
    }
  };

  /**
   * Return cached metadata for the given entity class, creating it if necessary.
   *
   * <p>On a cache miss, delegates to {@link #createEntityMetadata(Class)} and caches
   * the result. Subsequent calls on this factory for the same class return the
   * cached instance. Failed creation attempts are not cached.
   *
   * @param entityClass the entity class to resolve
   * @return the cached or newly created entity metadata
   * @throws IllegalEntityException if a valid entity mapping cannot be determined
   * @see #createEntityMetadata(Class)
   */
  public EntityMetadata getEntityMetadata(Class<?> entityClass) throws IllegalEntityException {
    return entityCache.get(entityClass, this);
  }

  /**
   * Create metadata for the given entity class using this factory's mapping strategy.
   *
   * <p>Called by {@link #getEntityMetadata(Class)} on a cache miss. Calling this
   * method directly bypasses the factory's cache lookup and does not add the
   * returned metadata to the cache.
   *
   * @param entityClass the entity class to map
   * @return the newly created entity metadata, never {@code null}
   * @throws IllegalEntityException if a valid entity mapping cannot be determined
   * @see #getEntityMetadata(Class)
   */
  public abstract EntityMetadata createEntityMetadata(Class<?> entityClass) throws IllegalEntityException;

}
