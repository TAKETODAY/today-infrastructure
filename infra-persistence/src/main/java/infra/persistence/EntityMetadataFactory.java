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
 * An abstract factory class for creating and caching {@link EntityMetadata} instances.
 * This class provides a mechanism to retrieve or create metadata for entity classes,
 * ensuring that metadata is cached for efficient reuse.
 *
 * <p>Subclasses must implement the {@link #createEntityMetadata(Class)} method to define
 * how metadata is created for a given entity class.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 *  // Create a custom implementation of EntityMetadataFactory
 *  DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
 *
 *  // Configure the factory with necessary components
 *  factory.setTableNameGenerator(new CustomTableNameGenerator());
 *  factory.setIdPropertyDiscover(new CustomIdPropertyDiscover());
 *  factory.setPropertyFilter(new CustomPropertyFilter());
 *  factory.setColumnNameDiscover(new CustomColumnNameDiscover());
 *  factory.setTypeHandlerManager(new CustomTypeHandlerManager());
 *
 *  // Retrieve metadata for an entity class
 *  try {
 *    Class<?> entityClass = MyEntity.class;
 *    EntityMetadata metadata = factory.getEntityMetadata(entityClass);
 *    System.out.println("Entity Metadata: " + metadata);
 *  } catch (IllegalEntityException e) {
 *    System.err.println("Failed to retrieve entity metadata: " + e.getMessage());
 *  }
 * }</pre>
 *
 * <p>The above example demonstrates how to use a custom implementation of
 * {@code EntityMetadataFactory} to retrieve metadata for an entity class. The factory
 * is configured with various components that define how metadata is generated.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityMetadata
 * @see IllegalEntityException
 * @since 4.0 2022/8/16 23:28
 */
public abstract class EntityMetadataFactory {

  final MapCache<Class<?>, EntityMetadata, EntityMetadataFactory> entityCache = new MapCache<>() {

    @Override
    protected EntityMetadata createValue(Class<?> entityClass, EntityMetadataFactory entityMetadataFactory) {
      return entityMetadataFactory.createEntityMetadata(entityClass);
    }
  };

  /**
   * Retrieves the metadata for a given entity class from the cache. If the metadata is not already
   * cached, it will be generated using the {@link #createEntityMetadata(Class)} method and then stored
   * in the cache for future use.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   *   EntityMetadataFactory factory = new CustomEntityMetadataFactory();
   *
   *   try {
   *     Class<?> entityClass = MyEntity.class;
   *     EntityMetadata metadata = factory.getEntityMetadata(entityClass);
   *
   *     // Use the retrieved metadata
   *     System.out.println("Table Name: " + metadata.getTableName());
   *   }
   *   catch (IllegalEntityException e) {
   *     System.err.println("Failed to retrieve entity metadata: " + e.getMessage());
   *   }
   * }</pre>
   *
   * <p>In the example above, the {@code getEntityMetadata} method is used to retrieve metadata for
   * the {@code MyEntity} class. If the metadata is not already cached, it will be generated and
   * cached automatically.</p>
   *
   * @param entityClass the entity class for which metadata is to be retrieved
   * @return the {@link EntityMetadata} instance representing the metadata for the given entity class
   * @throws IllegalEntityException if the entity class is invalid or does not meet the requirements
   * for metadata generation
   */
  public EntityMetadata getEntityMetadata(Class<?> entityClass) throws IllegalEntityException {
    return entityCache.get(entityClass, this);
  }

  /**
   * Creates a new {@link EntityMetadata} instance for the given entity class.
   * This method is responsible for defining how metadata is generated for a specific
   * entity class. It is invoked by the caching mechanism when no cached metadata exists
   * for the provided entity class.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   *  // Implement the createEntityMetadata method in a subclass
   *  public class CustomEntityMetadataFactory extends EntityMetadataFactory {
   *    @Override
   *    public EntityMetadata createEntityMetadata(Class<?> entityClass) throws IllegalEntityException {
   *      if (!isValidEntity(entityClass)) {
   *        throw new IllegalEntityException("Invalid entity class: " + entityClass.getName());
   *      }
   *      // Generate metadata based on the entity class
   *      String tableName = generateTableName(entityClass);
   *      List<PropertyMetadata> properties = discoverProperties(entityClass);
   *      return new EntityMetadata(...);
   *    }
   *  }
   * }</pre>
   *
   * <p>In the example above, a custom implementation of {@code createEntityMetadata} is provided.
   * The method validates the entity class, generates a table name, and discovers properties to
   * construct an {@code EntityMetadata} instance.</p>
   *
   * @param entityClass the entity class for which metadata is to be created
   * @return a new {@link EntityMetadata} instance representing the metadata for the given entity class
   * @throws IllegalEntityException if the entity class is invalid or does not meet the requirements
   * for metadata generation
   */
  public abstract EntityMetadata createEntityMetadata(Class<?> entityClass) throws IllegalEntityException;

}
