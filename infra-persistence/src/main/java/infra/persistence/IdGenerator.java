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

/**
 * Strategy for generating entity primary-key values before insertion.
 *
 * <p>A generator is invoked after pre-persist callbacks when the ID property
 * is {@code null}. The persistence layer assigns the returned value to the
 * entity and includes it in the INSERT statement. Existing non-null IDs are
 * preserved without invoking the generator.
 *
 * <p>Implementations may generate IDs locally (for example, UUIDs or Snowflake
 * IDs) or obtain them from a database, Redis, or a remote ID service. The
 * implementation is responsible for uniqueness across its intended scope,
 * including coordination between nodes when required.
 *
 * <p>Generators may be selected by type or bean name through
 * {@link infra.persistence.annotation.GeneratedId @GeneratedId}, including
 * composed annotations.
 *
 * <p>{@link infra.persistence.support.IdGeneratorResolver} caches the first
 * successfully resolved instance for each generator type and bean-name
 * combination. Subsequent resolutions reuse that instance, even when the bean
 * is declared with prototype scope. This reuse is scoped to the resolver, not
 * a global singleton. Implementations must be thread-safe and must not retain
 * per-invocation entity state in instance fields.
 *
 * <p>Only generators created on demand are destroyed by the resolver; existing
 * beans are not destroyed by it. Generators that need to share node configuration
 * or allocated ID ranges across resolvers should be registered as
 * container-managed singleton beans.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface IdGenerator {

  /**
   * Generate a primary-key value for the given entity.
   * <p>The persistence layer assigns the returned value; implementations should
   * return the ID rather than modify the entity's ID property directly. An ID
   * allocated by this method is not reclaimed when insertion fails or the
   * transaction rolls back.
   *
   * @param entity the entity being inserted
   * @param metadata the mapping metadata for the entity
   * @param idProperty the primary-key property to receive the generated value
   * @return a non-null value assignable to {@link EntityProperty#getType() the
   * declared primary-key type}
   */
  Object generateId(Object entity, EntityMetadata metadata, EntityProperty idProperty);

}
