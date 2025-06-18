/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.core.Pair;
import infra.dao.DataAccessException;
import infra.lang.Nullable;
import infra.util.StreamIterable;

/**
 * The EntityManager class provides a comprehensive API for managing
 * entities within an underlying data repository. It supports operations
 * such as persisting, updating, deleting, and retrieving entities,
 * as well as performing bulk operations on collections or streams
 * of entities. The class ensures that entity metadata is validated during
 * operations and provides mechanisms to define persistence strategies
 * and handle auto-generated keys.
 *
 * <p>
 * The EntityManager is designed to interact with a variety of data sources,
 * abstracting the complexities of data access and manipulation. It enforces
 * strict validation of entity classes and properties, ensuring that only legal
 * entities are processed.
 *
 * <p>
 * <h2>Key Features:</h2>
 * <ul>
 *   <li> Persisting single entities or collections of entities with optional
 * strategies and auto-generated key handling. </li>
 *   <li>Updating entities based on their state, unique identifiers, or specific criteria.</li>
 *   <li>Deleting entities by ID, example, or truncating entire tables.</li>
 *   <li>Retrieving entities by ID, example, or finding the first match based on criteria.</li>
 * </ul>
 *
 * <p>
 * All methods throw DataAccessException in case of data access errors,
 * and IllegalEntityException is thrown when entity metadata parsing fails
 * or when an illegal entity is encountered.
 *
 * <p>
 * <h2>Notes:</h2>
 * <ul>
 *   <li>Persistence strategies allow fine-grained control over which properties are updated or persisted.</li>
 *   <li>Auto-generated key handling can be enabled or disabled based on requirements.</li>
 *   <li>Bulk operations support both Iterable and Stream inputs for flexibility.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:47
 */
public interface EntityManager {

  /**
   * Persists the given entity to the underlying data store.
   * This method is typically used to save a new entity or update
   * an existing one, depending on the implementation and the state
   * of the entity.
   *
   * <p>Example usage:
   * <pre>{@code
   *   User user = new User("JohnDoe", "john.doe@example.com");
   *
   *   try {
   *     int rowsAffected = entityManager.persist(user);
   *     System.out.println("Rows updated: " + rowsAffected);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Failed to persist user: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param entity the entity to be persisted; must not be null.
   * The exact type and structure of the entity depend  on the specific implementation.
   * @return the number of rows affected by the persistence operation
   * are determined by the underlying persistence mechanism.
   * @throws DataAccessException if an error occurs while accessing
   * the data store during the persist operation.
   */
  int persist(Object entity) throws DataAccessException;

  /**
   * Persists the given entity to the underlying data store using the specified
   * update strategy. If no strategy is provided, a default strategy will be used.
   *
   * <p>This method is typically used to save or update an entity in the database.
   * The behavior of the persistence operation can be customized by providing a
   * {@link PropertyUpdateStrategy}. If the operation fails, a
   * {@link DataAccessException} is thrown.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   *   User user = new User("JohnDoe", "john.doe@example.com");
   *   try {
   *     int affectedRows = entityManager.persist(user, PropertyUpdateStrategy.always());
   *     System.out.println("Affected rows: " + affectedRows);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Failed to persist user: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param entity the entity to be persisted; must not be null
   * @param strategy the strategy to use for updating properties, or null to use
   * the default strategy
   * @return the number of rows affected by the persistence operation
   * @throws DataAccessException if an error occurs during the persistence process
   */
  int persist(Object entity, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Persists the given entity to the data store. If the {@code autoGenerateId}
   * parameter is set to true, the method will automatically generate an ID for
   * the entity if it does not already have one. Otherwise, the provided ID will
   * be used as-is.
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *   // Persist an entity with automatic ID generation
   *   MyEntity entity = new MyEntity();
   *   entity.setName("Sample Entity");
   *   int rowsAffected = entityManager.persist(entity, true);
   *
   *   // Persist an entity with a manually assigned ID
   *   MyEntity entityWithId = new MyEntity();
   *   entityWithId.setId(123);
   *   entityWithId.setName("Another Entity");
   *   int rowsAffected = entityManager.persist(entityWithId, false);
   * }</pre>
   *
   * @param entity the entity object to be persisted; must not be null
   * @param autoGenerateId whether to automatically generate an ID for the entity
   * if it does not already have one
   * @return the number of rows affected in the data store as a result of the
   * persistence operation
   * @throws DataAccessException if an error occurs while accessing the data store
   */
  int persist(Object entity, boolean autoGenerateId) throws DataAccessException;

  /**
   * Persists the given entity to the underlying data store using the specified
   * persistence strategy and ID generation option.
   * <p>
   * This method allows fine-grained control over how the entity is persisted.
   * The {@code strategy} parameter determines how properties of the entity are
   * updated, while the {@code autoGenerateId} flag specifies whether an ID should
   * be automatically generated for the entity if it does not already have one.
   * <p>
   * Example usage:
   * <pre>{@code
   *   User user = new User();
   *   user.setName("John Doe");
   *   user.setEmail("john.doe@example.com");
   *
   *   // Persist the user with auto-generated ID and default update strategy
   *   int rowsAffected = entityManager.persist(user, null, true);
   *
   *   System.out.println("Rows affected: " + rowsAffected);
   *   System.out.println("User id: " + user.getId());
   * }</pre>
   *
   * @param entity the entity to be persisted; must not be null
   * @param strategy the strategy to use for updating properties of the entity;
   * can be null to indicate the default strategy should be used
   * @param autoGenerateId a boolean flag indicating whether an ID should be
   * automatically generated for the entity if it lacks one
   * @return the number of rows affected by the persistence operation
   * @throws DataAccessException if there is an issue accessing the data store
   * during the persistence process
   */
  int persist(Object entity, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException;

  /**
   * Persists the given collection of entities into the underlying data store.
   * This method is typically used to save multiple objects in a single operation,
   * ensuring efficient batch processing where applicable.
   *
   * <p>Example usage:</p>
   * <pre><code>
   *   List&lt;User&gt; users = Arrays.asList(
   *     new User("Alice"),
   *     new User("Bob")
   *   );
   *
   *   try {
   *     entityManager.persist(users);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error persisting entities: " + e.getMessage());
   *   }
   * </code></pre>
   *
   * @param entities an {@link Iterable} of entities to be persisted. Each entity
   * must not be null and should conform to the data model structure
   * expected by the data store.
   * @throws DataAccessException if there is an issue while interacting with the
   * underlying data store during the persist operation.
   */
  void persist(Iterable<?> entities) throws DataAccessException;

  /**
   * Persists the given entities to the underlying data store.
   * This method allows specifying whether the IDs of the entities
   * should be auto-generated or not.
   *
   * <p>If {@code autoGenerateId} is set to {@code true}, the data store
   * will generate unique identifiers for each entity that does not already
   * have an ID assigned. Otherwise, the existing IDs will be used.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * List<MyEntity> entities = Arrays.asList(
   *   new MyEntity("entity1"),
   *   new MyEntity("entity2")
   * );
   * try {
   *   entityManager.persist(entities, true);
   *   System.out.println("Entities persisted successfully.");
   * }
   * catch (DataAccessException e) {
   *   System.err.println("Failed to persist entities: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param entities an iterable collection of entities to be persisted;
   * must not be null
   * @param autoGenerateId a boolean flag indicating whether IDs should be
   * auto-generated for entities that do not have them
   * @throws DataAccessException if an error occurs while accessing or writing
   * to the data store
   */
  void persist(Iterable<?> entities, boolean autoGenerateId)
          throws DataAccessException;

  /**
   * Persists the given entities into the underlying data store using the specified
   * property update strategy. If no strategy is provided, a default strategy will be
   * applied. This method is typically used for batch operations where multiple entities
   * need to be saved or updated in a single call.
   *
   * <p>Example usage:
   * <pre>{@code
   * List<MyEntity> entities = Arrays.asList(
   *   new MyEntity("id1", "value1"),
   *   new MyEntity("id2", "value2")
   * );
   *
   * try {
   *   entityManager.persist(entities, PropertyUpdateStrategy.always());
   * }
   * catch (DataAccessException e) {
   *   // Handle exceptions related to data access
   *   e.printStackTrace();
   * }
   * }</pre>
   *
   * @param entities an iterable collection of entities to be persisted. Each entity
   * must be compatible with the data store's requirements. Must not
   * be null, but can be empty.
   * @param strategy the strategy to use for updating properties of existing entities
   * in the data store. If null, a default strategy defined by the
   * implementation will be used.
   * @throws DataAccessException if there is any issue accessing the data store during
   * the persistence operation.
   */
  void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Persists the given entities into the data store using the specified strategy and settings.
   * This method allows batch persistence of entities, making it suitable for scenarios where
   * multiple records need to be saved efficiently.
   *
   * <p>If {@code autoGenerateId} is set to true, the method will automatically generate unique
   * identifiers for entities that do not already have one. Otherwise, the existing identifiers
   * will be used.</p>
   *
   * <p>The {@code strategy} parameter defines how properties of the entities are updated during
   * persistence. If null, a default strategy will be applied.</p>
   *
   * <p><b>Example Usage:</b></p>
   *
   * <pre>{@code
   * List<MyEntity> entities = Arrays.asList(
   *   new MyEntity("entity1"),
   *   new MyEntity("entity2")
   * );
   *
   * try {
   *   entityManager.persist(entities, PropertyUpdateStrategy.always(), true);
   * }
   * catch (DataAccessException e) {
   *   logger.error("Failed to persist entities", e);
   * }
   * }</pre>
   *
   * @param entities the collection of entities to be persisted; must not be null
   * @param strategy the strategy to use for property updates; can be null to use the default
   * @param autoGenerateId whether to automatically generate IDs for entities without one
   * @throws DataAccessException if an error occurs while accessing the data store
   */
  void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException;

  /**
   * Persists a stream of entities into the underlying data store.
   * This method takes a stream of entities and delegates the persistence
   * operation to an overloaded {@code persist} method that accepts an
   * {@link Iterable}. The stream is wrapped using a {@link StreamIterable}.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Stream<MyEntity> entityStream = Stream.of(new MyEntity("id1"), new MyEntity("id2"));
   *   try {
   *     entityManager.persist(entityStream);
   *   }
   *   catch (DataAccessException e) {
   *     // Handle exception
   *   }
   * }</pre>
   *
   * @param entities the stream of entities to be persisted; must not be null
   * @throws DataAccessException if there is an issue during the persistence process
   */
  default void persist(Stream<?> entities) throws DataAccessException {
    persist(new StreamIterable<>(entities));
  }

  /**
   * Persists a stream of entities into the data store. If {@code autoGenerateId} is set to true,
   * the method will automatically generate identifiers for entities that do not already have them.
   * This method wraps the provided stream into an {@link Iterable} and delegates the persistence
   * operation to the overloaded {@link #persist(Iterable, boolean)} method.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Stream<MyEntity> entityStream = Stream.of(new MyEntity("data1"), new MyEntity("data2"));
   *
   *   try {
   *     entityManager.persist(entityStream, true);
   *     System.out.println("Entities persisted successfully.");
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Failed to persist entities: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param entities a stream of entities to be persisted; must not be null
   * @param autoGenerateId a flag indicating whether to automatically generate IDs for entities
   * without identifiers; if true, ID generation is enabled
   * @throws DataAccessException if an error occurs during the persistence process, such as
   * database connectivity issues or constraint violations
   */
  default void persist(Stream<?> entities, boolean autoGenerateId) throws DataAccessException {
    persist(new StreamIterable<>(entities), autoGenerateId);
  }

  /**
   * Persists a stream of entities into the data store using the specified update strategy.
   * If no strategy is provided, a default strategy will be used. This method wraps the
   * incoming stream into an {@link Iterable} and delegates the persistence operation to
   * the overloaded {@link #persist(Iterable, PropertyUpdateStrategy)} method.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Stream<MyEntity> entityStream = Stream.of(entity1, entity2, entity3);
   *   try {
   *     entityManager.persist(entityStream, PropertyUpdateStrategy.always());
   *   }
   *   catch (DataAccessException e) {
   *     // Handle exceptions related to data access
   *     logger.error("Error persisting entities", e);
   *   }
   * }</pre>
   *
   * <p>Note: The caller should ensure proper handling of the stream lifecycle to avoid
   * resource leaks. For example, closing the stream after the operation completes is
   * recommended if the stream is not automatically closed by the implementation.
   *
   * @param entities a stream of entities to be persisted; must not be null
   * @param strategy the strategy to use for property updates during persistence;
   * can be null, in which case a default strategy will be applied
   * @throws DataAccessException if an error occurs during the persistence process
   */
  default void persist(Stream<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(new StreamIterable<>(entities), strategy);
  }

  /**
   * Persists a stream of entities into the underlying data store using the specified
   * persistence strategy and auto-generation setting for entity IDs.
   * <p>
   * This method wraps the provided stream into an {@link Iterable} and delegates the
   * persistence operation to the overloaded {@link #persist(Iterable, PropertyUpdateStrategy, boolean)}
   * method. If the stream is null or empty, no action will be taken.
   * <p>
   * Example usage:
   * <pre>{@code
   *   Stream<MyEntity> entities = Stream.of(new MyEntity("entity1"), new MyEntity("entity2"));
   *
   *   // Persist entities with auto-generated IDs and a custom update strategy
   *   entityManager.persist(entities, PropertyUpdateStrategy.MERGE, true);
   * }</pre>
   *
   * @param entities a stream of entities to be persisted; must not be null
   * @param strategy the strategy to use for updating properties during persistence;
   * can be null if no specific strategy is required
   * @param autoGenerateId indicates whether IDs should be automatically generated for
   * entities that do not already have one
   * @throws DataAccessException if an error occurs during the persistence process
   */
  default void persist(Stream<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId) throws DataAccessException {
    persist(new StreamIterable<>(entities), strategy, autoGenerateId);
  }

  /**
   * Updates the given entity or example in the database. This method is typically
   * used to modify existing records based on the provided object's state.
   *
   * <p>Example usage:
   * <pre>{@code
   *   User user = new User();
   *   user.setId(1);
   *   user.setName("John Doe");
   *
   *   int rowsAffected = entityManager.update(user);
   *   System.out.println("Rows updated: " + rowsAffected);
   * }</pre>
   *
   * @param entityOrExample the object representing the entity or example to be updated.
   * It should contain the necessary fields to identify the record(s)
   * to update, along with the new values to apply.
   * @return the number of rows affected by the update operation. A return value
   * greater than zero indicates successful updates.
   * @throws DataAccessException if there is any issue accessing the data layer, such
   * as database connection errors or invalid query execution.
   */
  int update(Object entityOrExample) throws DataAccessException;

  /**
   * Updates the given entity or example in the database using the specified update strategy.
   * If no strategy is provided, a default strategy will be used. This method is typically
   * used to modify existing records in the database based on the provided entity or example.
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   *   // Update an entity with a custom strategy
   *   MyEntity entity = new MyEntity();
   *   entity.setId(1);
   *   entity.setName("Updated Name");
   *
   *   PropertyUpdateStrategy strategy = PropertyUpdateStrategy.noneNull();
   *
   *   int rowsAffected = entityManager.update(entity, strategy);
   *   System.out.println("Rows updated: " + rowsAffected);
   *
   *   // Update using only an example (without a strategy)
   *   Example example = Example.builder()
   *       .field("status", "inactive")
   *       .build();
   *
   *   rowsAffected = repository.update(example, null);
   *   System.out.println("Rows updated: " + rowsAffected);
   * }</pre>
   *
   * @param entityOrExample the entity or example object containing the data to update.
   * This can be a full entity or a partial example object.
   * @param strategy the optional update strategy to apply. If null, a default
   * strategy will be used.
   * @return the number of rows affected by the update operation.
   * @throws DataAccessException if there is any issue accessing the database during
   * the update process.
   */
  int update(Object entityOrExample, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Updates an existing record in the database by its ID using the provided
   * entity or example object. The method attempts to match the record based
   * on the ID field of the given object and updates the corresponding fields.
   *
   * <p>Example usage:
   * <pre>{@code
   *   User user = new User();
   *   user.setId(10);
   *   user.setName("John Doe");
   *   user.setEmail("john.doe@example.com");
   *
   *   int rowsAffected = entityManager.updateById(user);
   *   if (rowsAffected > 0) {
   *     System.out.println("Update successful.");
   *   }
   *   else {
   *     System.out.println("No records were updated.");
   *   }
   * }</pre>
   *
   * @param entityOrExample the object containing the ID and updated fields.
   * It must not be null, and the ID field should be set.
   * @return the number of rows affected by the update operation. Returns 0 if
   * no matching record is found.
   * @throws DataAccessException if there is any issue accessing the database
   * during the update process.
   */
  int updateById(Object entityOrExample) throws DataAccessException;

  /**
   * Updates an entity in the database by its ID using the provided
   * entity or example object. The method matches the given ID with
   * the corresponding record in the database and applies the update
   * based on the fields of the provided object.
   *
   * <p>Example usage:</p>
   *
   * <pre><code>
   * User user = new User();
   * user.setName("John Doe");
   * user.setEmail("john.doe@example.com");
   *
   * int rowsUpdated = entityManager.updateById(user, 123);
   * if (rowsUpdated > 0) {
   *   System.out.println("Update successful!");
   * }
   * else {
   *   System.out.println("No rows were updated.");
   * }
   * </code></pre>
   *
   * @param entityOrExample the object containing the fields to update.
   * This can be a full entity or an example object with partial fields.
   * @param id the unique identifier of the record to be updated in the database.
   * @return the number of rows affected by the update operation.
   * Returns 0 if no matching record is found.
   * @throws DataAccessException if there is any issue accessing the database
   * during the update process.
   */
  int updateById(Object entityOrExample, Object id) throws DataAccessException;

  /**
   * Updates an entity in the database by its ID using the provided entity or example object.
   * The update behavior can be customized using the optional {@code strategy} parameter.
   *
   * <p>This method attempts to locate the record by the ID extracted from the provided
   * {@code entityOrExample} and applies the update accordingly. If no matching record
   * is found, the behavior depends on the implementation.</p>
   *
   * <p><b>Example Usage:</b></p>
   *
   * <pre>{@code
   * // Update an entity with default strategy
   * User user = new User();
   * user.setId(1);
   * user.setName("Updated Name");
   * int rowsAffected = entityManager.updateById(user, null);
   * System.out.println(rowsAffected + " row(s) updated.");
   *
   * // Update with a custom strategy
   * PropertyUpdateStrategy strategy = (property, value) -> {
   *   return value != null; // Only update non-null properties
   * };
   * int rowsAffectedWithStrategy = entityManager.updateById(user, strategy);
   * System.out.println(rowsAffectedWithStrategy + " row(s) updated with strategy.");
   * }</pre>
   *
   * @param entityOrExample the object containing the ID and updated field values,
   * or an example object defining the update criteria
   * @param strategy the optional strategy to control which properties are updated;
   * if null, all properties will be updated by default
   * @return the number of rows affected by the update operation
   * @throws DataAccessException if there is any issue accessing the data layer
   */
  int updateById(Object entityOrExample, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Updates an entity in the database by its ID using the provided entity or example object.
   * The method allows for selective property updates based on the specified update strategy.
   *
   * <p>Example usage:
   * <pre>{@code
   *   User user = new User();
   *   user.setName("John Doe");
   *   user.setEmail("john.doe@example.com");
   *
   *   int rowsAffected = entityManager.updateById(user, 123L, PropertyUpdateStrategy.always());
   *   if (rowsAffected > 0) {
   *     System.out.println("Update successful!");
   *   }
   *   else {
   *     System.out.println("No rows were updated.");
   *   }
   * }</pre>
   *
   * @param entityOrExample the object containing the updated values or an example
   * object to match fields for the update operation
   * @param id the unique identifier of the entity to be updated in the database
   * @param strategy the optional strategy defining how properties should be updated;
   * can be null to use the default strategy
   * @return the number of rows affected by the update operation
   * @throws DataAccessException if there is any issue accessing the data source during
   * the update process
   */
  int updateById(Object entityOrExample, Object id, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Updates records in the database based on the provided entity or example and a custom WHERE clause.
   * The method allows for flexible updates by accepting an object representing the updated values
   * and a WHERE condition to specify which records should be affected.
   *
   * @param entityOrExample an instance of Object containing the fields to be updated.
   * This can either represent a full entity or an example with partial fields.
   * @param where a String representing the WHERE clause of the update query.
   * This should follow SQL syntax conventions and can include placeholders if needed.
   * @return the number of rows affected by the update operation.
   * @throws DataAccessException if there is an issue accessing the database or executing the update.
   */
  int updateBy(Object entityOrExample, String where) throws DataAccessException;

  /**
   * Updates records in the database based on the provided entity or example object,
   * with an optional WHERE clause and a specified update strategy.
   *
   * <p>This method allows for flexible updates by accepting either an entity instance
   * or an example object to define the update criteria. The WHERE clause can be used
   * to further refine the selection of records to be updated. Additionally, a custom
   * update strategy can be applied to control how properties are updated.
   *
   * @param entityOrExample the entity or example object that defines the update criteria.
   * If an entity is provided, its non-null fields are used for updating.
   * If an example is provided, it serves as a template for matching records.
   * @param where a String representing the WHERE clause of the update query.
   * This should follow SQL syntax conventions and can include placeholders if needed.
   * @param strategy the optional update strategy that determines how properties
   * are updated. If null, a default strategy is applied.
   * @return the number of rows affected by the update operation.
   * @throws DataAccessException if there is an issue accessing the database during the update process.
   */
  int updateBy(Object entityOrExample, String where, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Saves a new entity or updates an existing one in the data store.
   * If the entity already exists (typically determined by its unique identifier),
   * it will be updated. Otherwise, a new entity will be created.
   *
   * Example usage:
   * <pre>{@code
   *   User user = new User();
   *   user.setId(1);
   *   user.setName("John Doe");
   *
   *   try {
   *     int rowsAffected = entityManager.saveOrUpdate(user);
   *     System.out.println(rowsAffected + " row(s) affected.");
   *   } catch (DataAccessException e) {
   *     System.err.println("Error while saving or updating: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param entity the entity object to be saved or updated; must not be null
   * @return the number of rows affected in the data store as a result of the
   * save or update operation
   * @throws DataAccessException if there is an issue accessing the data store
   * @see #persist(Object)
   * @see #update(Object)
   * @see NewEntityIndicator
   * @since 5.0
   */
  int saveOrUpdate(Object entity) throws DataAccessException;

  /**
   * Saves a new entity or updates an existing one in the data store.
   * If the entity already exists (typically determined by its primary key),
   * it will be updated. Otherwise, a new record will be created.
   *
   * <p>This method allows specifying a {@link PropertyUpdateStrategy} to control
   * how properties of the entity are updated. If no strategy is provided, a default
   * strategy will be applied.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * Person person = new Person();
   * person.setId(1);
   * person.setName("John Doe");
   *
   * // Save or update the entity with a custom update strategy
   * PropertyUpdateStrategy strategy = PropertyUpdateStrategy.noneNull();
   * int affectedRows = entityManager.saveOrUpdate(person, strategy);
   * System.out.println("Affected rows: " + affectedRows);
   *
   * // Save or update without specifying a strategy
   * int affectedRowsDefault = entityManager.saveOrUpdate(person, null);
   * System.out.println("Affected rows (default strategy): " + affectedRowsDefault);
   * }</pre>
   *
   * @param entity the entity object to be saved or updated; must not be null
   * @param strategy the strategy to use for updating properties, or null to use
   * the default strategy
   * @return the number of rows affected in the data store (e.g., 1 for
   * a successful save/update, 0 if no changes were made)
   * @throws DataAccessException if there is an error accessing the data store
   * @see #persist(Object, PropertyUpdateStrategy)
   * @see #update(Object, PropertyUpdateStrategy)
   * @see NewEntityIndicator
   * @since 5.0
   */
  int saveOrUpdate(Object entity, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Deletes the entity with the specified identifier from the database.
   * The entity to be deleted is determined by the provided entity class and
   * its unique identifier (id). If no matching entity is found, the behavior
   * depends on the underlying data access implementation.
   *
   * Example usage:
   * <pre>{@code
   *   // Assuming a User entity with ID 101 exists in the database
   *   int rowsAffected = entityManager.delete(User.class, 101);
   *   if (rowsAffected > 0) {
   *     System.out.println("Entity deleted successfully.");
   *   }
   *   else {
   *     System.out.println("No entity found with the given ID.");
   *   }
   * }</pre>
   *
   * @param entityClass the class of the entity to be deleted. Must not be null.
   * @param id the unique identifier of the entity to be deleted. Must not be null.
   * @return the number of rows affected by the delete operation. Typically, this
   * will be 1 if the entity was found and deleted, or 0 if no matching
   * entity was found.
   * @throws DataAccessException if there is any problem accessing the database.
   * This could include issues like connection errors or constraint violations.
   */
  int delete(Class<?> entityClass, Object id) throws DataAccessException;

  /**
   * Deletes the given entity or example from the data store.
   * This method accepts either an entity instance representing a row
   * in the database or an example object used for matching records.
   *
   * <p>Example usage:</p>
   * <pre>{@code
   *  // Deleting an entity
   *  MyEntity entity = new MyEntity();
   *  entity.setId(123);
   *  int rowsDeleted = entityManager.delete(entity);
   *
   *  // Deleting by example (e.g., using a partial object)
   *  MyEntity example = new MyEntity();
   *  example.setStatus("inactive");
   *  int rowsDeletedByExample = entityManager.delete(example);
   * }</pre>
   *
   * @param entityOrExample the entity instance or example object
   * to be deleted; must not be null
   * @return the number of rows affected (deleted) in the data store
   * @throws DataAccessException if there is any problem accessing the data store
   */
  int delete(Object entityOrExample) throws DataAccessException;

  /**
   * Truncates all records from the table associated with the specified entity class.
   * This operation is equivalent to executing a "TRUNCATE TABLE" SQL statement,
   * which removes all rows from the table but retains the table structure.
   *
   * <p><b>Note:</b> This method does not delete individual rows but clears the entire
   * table content. Use this with caution as it cannot be undone.</p>
   *
   * <p><b>Example Usage:</b></p>
   *
   * <pre>{@code
   *  // Assuming we have an entity class named 'User'
   *  try {
   *    entityManager.truncate(User.class);
   *    System.out.println("All records in the User table have been truncated.");
   *  }
   *  catch (DataAccessException e) {
   *    System.err.println("An error occurred while truncating the table: " + e.getMessage());
   *  }
   * }</pre>
   *
   * @param entityClass the class object representing the entity whose associated
   * table will be truncated. Must not be null.
   * @throws DataAccessException if there is any issue accessing or modifying the data
   * in the database during the truncate operation.
   * @since 5.0
   */
  void truncate(Class<?> entityClass) throws DataAccessException;

  /**
   * Retrieves an entity of the specified class by its unique identifier.
   * If no entity is found for the given ID, {@code null} is returned.
   *
   * <p><b>Usage Example:</b></p>
   *
   * Assuming a persistent entity {@code User} with a primary key of type {@code Long}:
   * <pre>{@code
   *   Long userId = 1L;
   *   User user = entityManager.findById(User.class, userId);
   *
   *   if (user != null) {
   *     System.out.println("User found: " + user.getName());
   *   }
   *   else {
   *     System.out.println("User not found.");
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity to retrieve
   * @param entityClass the class of the entity to retrieve (must not be {@code null})
   * @param id the unique identifier of the entity to find (must not be {@code null})
   * @return the entity with the given ID, or {@code null} if none is found
   * @throws DataAccessException if there is any issue accessing the underlying data store
   */
  @Nullable
  <T> T findById(Class<T> entityClass, Object id) throws DataAccessException;

  /**
   * Searches for the first occurrence of the specified entity in the data store.
   * If a match is found, it returns the entity; otherwise, it returns {@code null}.
   * This method is generic and can be used with any type of entity.
   *
   * <p>Example usage:</p>
   * <pre><code>
   *   User user = new User();
   *   user.setId(1L);
   *
   *   User result = entityManager.findFirst(user);
   *   if (result != null) {
   *     System.out.println("User found: " + result.getName());
   *   }
   *   else {
   *     System.out.println("No user found.");
   *   }
   * </code></pre>
   *
   * @param <T> the type of the entity to search for
   * @param entity the entity to look for in the data store. Must not be {@code null}.
   * @return the first matching entity found in the data store, or {@code null} if
   * no match is found
   * @throws DataAccessException if there is an issue accessing the data store
   */
  @Nullable
  <T> T findFirst(T entity) throws DataAccessException;

  /**
   * Searches for the first entity of the specified type that matches the given example.
   * This method is typically used to retrieve a single entity based on a prototype or
   * example object containing matching criteria.
   *
   * <p>Example usage:
   * <pre>{@code
   * User exampleUser = new User();
   * exampleUser.setActive(true);
   *
   * User firstActiveUser = entityManager.findFirst(User.class, exampleUser);
   * if (firstActiveUser != null) {
   *   System.out.println("Found user: " + firstActiveUser.getName());
   * }
   * else {
   *   System.out.println("No active user found.");
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class object representing the type of entity to search for
   * @param example an instance of an object containing the matching criteria;
   * non-null fields are used as conditions for the search
   * @return the first matching entity of type T, or null if no match is found
   * @throws DataAccessException if there is any issue accessing the data store during
   * the search operation
   */
  @Nullable
  <T> T findFirst(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * Finds the first entity of the specified type that matches the given query conditions.
   * This method is typically used to retrieve a single result from the database based on
   * the provided query handler. If no matching entity is found, {@code null} is returned.
   *
   * <p>Example usage:
   * <pre>{@code
   * QueryStatement query = qb -> qb.where("status").is("active");
   * User user = entityManager.findFirst(User.class, query);
   * if (user != null) {
   *   System.out.println("Found user: " + user.getName());
   * }
   * else {
   *   System.out.println("No active user found.");
   * }
   * }</pre>
   *
   * <p>If the query handler is {@code null}, the method will attempt to retrieve the first
   * entity without any specific filtering, which may result in an arbitrary match.
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class object representing the type of entity to search for
   * @param handler the query statement defining the search conditions; can be {@code null}
   * @return the first matching entity of the specified type, or {@code null} if
   * no match is found
   * @throws DataAccessException if there is an issue accessing the underlying data store
   */
  @Nullable
  <T> T findFirst(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * Finds a unique object in the data store that matches the given example.
   * If no match is found, returns {@code null}. If more than one match is found,
   * a {@link DataAccessException} is thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   * User exampleUser = new User();
   * exampleUser.setEmail("user@example.com");
   *
   * User uniqueUser = entityManager.findUnique(exampleUser);
   * if (uniqueUser != null) {
   *   System.out.println("Found user: " + uniqueUser.getName());
   * }
   * else {
   *   System.out.println("No user found with the given email.");
   * }
   * }</pre>
   *
   * @param <T> the type of the object to search for
   * @param example an instance of T representing the example object to match;
   * should not be {@code null}
   * @return the unique object matching the example, or {@code null} if
   * no match is found
   * @throws DataAccessException if more than one match is found or if there is
   * an issue accessing the data store
   */
  @Nullable
  <T> T findUnique(T example) throws DataAccessException;

  /**
   * Finds a unique entity of the specified type that matches the given example.
   * This method is typically used to query a database for a single result
   * based on an example object. If no matching entity is found, the method
   * returns {@code null}. If more than one matching entity exists, a
   * {@link DataAccessException} is thrown.
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * User exampleUser = new User();
   * exampleUser.setEmail("example@example.com");
   *
   * User uniqueUser = entityManager.findUnique(User.class, exampleUser);
   * if (uniqueUser != null) {
   *   System.out.println("Found user: " + uniqueUser.getName());
   * }
   * else {
   *   System.out.println("No user found with the given email.");
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class of the entity to be retrieved; must not be null
   * @param example an example object used as a query template; properties
   * set in this object will be used to match entities
   * @return the unique entity that matches the example, or {@code null} if no
   * such entity exists
   * @throws DataAccessException if more than one entity matches the example or
   * if there is an issue accessing the data source
   */
  @Nullable
  <T> T findUnique(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * Finds a unique entity of the specified class using the provided query handler.
   * If no entity is found, returns {@code null}. If more than one entity matches
   * the query criteria, a {@link DataAccessException} is thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   * QueryStatement query = q -> q.where("name", isEqualTo("John Doe"));
   * User user = entityManager.findUnique(User.class, query);
   * if (user != null) {
   *   System.out.println("Found user: " + user.getName());
   * }
   * else {
   *   System.out.println("No user found.");
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class of the entity to be retrieved; must not be {@code null}
   * @param handler the query statement used to define search criteria;
   * can be {@code null} to retrieve any single entity of the type
   * @return the unique entity matching the query criteria, or {@code null}
   * if no matching entity is found
   * @throws DataAccessException if more than one entity matches the query criteria
   * or if there is an issue accessing the data source
   */
  @Nullable
  <T> T findUnique(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * Finds all entities of the specified class from the data store.
   * <p>This method retrieves a list of all persisted instances of the given entity class.
   * If no entities are found, an empty list is returned.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Assuming EntityManager is properly initialized
   *   try {
   *     List<User> users = entityManager.find(User.class);
   *     for (User user : users) {
   *       System.out.println(user.getName());
   *     }
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error occurred while fetching data: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity class to be queried
   * @param entityClass the Class object representing the entity type to retrieve
   * @return a list of all persisted instances of the specified entity class;
   * an empty list if no entities are found
   * @throws DataAccessException if there is an issue accessing the data store
   */
  <T> List<T> find(Class<T> entityClass) throws DataAccessException;

  /**
   * Finds and returns a list of entities of the specified type, sorted
   * according to the provided sort keys. This method is typically used to
   * retrieve data from a data store with custom sorting options.
   *
   * <p>Example usage:
   * <pre>{@code
   * Map<String, Order> sortKeys = new HashMap<>();
   * sortKeys.put("name", Order.ASC);
   * sortKeys.put("age", Order.DESC);
   *
   * try {
   *   List<Person> people = entityManager.find(Person.class, sortKeys);
   *   people.forEach(System.out::println);
   * }
   * catch (DataAccessException e) {
   *   System.err.println("Error while fetching data: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class object representing the type of entity to find
   * @param sortKeys a map where keys are field names and values define
   * the sort order for those fields
   * @return a list of entities of type T, sorted based on the provided sort keys
   * @throws DataAccessException if there is an issue accessing the data store
   */
  <T> List<T> find(Class<T> entityClass, Map<String, Order> sortKeys)
          throws DataAccessException;

  /**
   * Finds and returns a list of entities of the specified class, sorted
   * according to the provided sort key. This method is typically used to
   * retrieve data from a data store with a specific ordering.
   *
   * Example usage:
   * <pre>{@code
   *   Pair<String, Order> sortKey = new Pair<>("createdAt", Order.DESC);
   *   try {
   *     List<User> users = entityManager.find(User.class, sortKey);
   *     users.forEach(user -> System.out.println(user.getName()));
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error while fetching data: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class of the entity to be retrieved; this
   * determines the type of objects returned in the list
   * @param sortKey a pair containing the field name and the order
   * (ascending or descending) to sort the results by
   * @return a list of entities of type T, sorted based on the provided
   * sort key; returns an empty list if no data is found
   * @throws DataAccessException if there is an issue accessing the data
   * store during the operation
   */
  <T> List<T> find(Class<T> entityClass, Pair<String, Order> sortKey)
          throws DataAccessException;

  /**
   * Finds and retrieves a list of entities of the specified type, sorted
   * according to the provided sort keys. This method allows dynamic sorting
   * by accepting multiple sort key-value pairs.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Assuming we have an entity class `User` and want to sort by "name" ascending
   *   Class<User> userClass = User.class;
   *   Pair<String, Order> sortKey = new Pair<>("name", Order.ASC);
   *
   *   try {
   *     List<User> users = entityManager.find(userClass, sortKey);
   *     users.forEach(System.out::println);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error occurred while fetching data: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity to be retrieved
   * @param entityClass the class object representing the entity type to query
   * @param sortKeys variable number of pairs where the key is the field name
   * and the value is the sort order (ascending or descending)
   * @return a list of entities of type T, sorted based on the provided sort keys
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <T> List<T> find(Class<T> entityClass, Pair<String, Order>... sortKeys)
          throws DataAccessException;

  /**
   * Searches for entities matching the given example object.
   *
   * This method retrieves a list of entities that match the properties
   * defined in the provided example object. It performs a query based on
   * the non-null fields of the example, effectively filtering results.
   *
   * Example usage:
   * <pre>{@code
   *   User userExample = new User();
   *   userExample.setActive(true);
   *   userExample.setRole("ADMIN");
   *
   *   try {
   *     List<User> activeAdmins = entityManager.find(userExample);
   *     activeAdmins.forEach(System.out::println);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error while fetching data: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity to search for
   * @param example an instance of T containing fields to use as search criteria;
   * non-null fields are used for filtering
   * @return a list of entities of type T that match the example criteria;
   * returns an empty list if no matches are found
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> List<T> find(T example) throws DataAccessException;

  /**
   * Finds all instances of the specified entity class that match the given
   * example object. This method is typically used for querying data based on
   * partial criteria provided in the example object.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Define an example object with partial fields set
   *   User exampleUser = new User();
   *   exampleUser.setActive(true);
   *
   *   // Find all active users
   *   List<User> activeUsers = entityManager.find(User.class, exampleUser);
   *
   *   // Iterate through the results
   *   for (User user : activeUsers) {
   *     System.out.println(user.getName());
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity to be queried
   * @param entityClass the class of the entity to be queried; must not be null
   * @param example an instance of the entity class containing query
   * criteria; properties set in this object will be used to filter results
   * @return a list of entities that match the criteria specified in the
   * example object; returns an empty list if no matches are found
   * @throws DataAccessException if there is any problem accessing the data layer
   */
  <T> List<T> find(Class<T> entityClass, Object example)
          throws DataAccessException;

  /**
   * Finds and retrieves a list of entities of the specified class, optionally filtered by a query statement.
   * This method allows querying a data source for entities of type {@code T} using a custom query handler.
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * // Define a QueryStatement to filter results
   * QueryStatement query = new Query("age > ?", 18);
   *
   * // Find all User entities older than 18
   * List<User> users = entityManager.find(User.class, query);
   *
   * // Find all User entities without any filtering
   * List<User> allUsers = entityManager.find(User.class, null);
   * }</pre>
   *
   * @param <T> the type of the entity class to be queried
   * @param entityClass the class object representing the type of entity to retrieve
   * @param handler an optional query statement used to filter results; pass {@code null} to retrieve all entities
   * @return a list of entities of type {@code T} matching the query, or all entities if no query is provided
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> List<T> find(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * Searches for entities matching the given example and organizes the results into a map.
   * The map is keyed by the specified property of the entities, as defined by the `mapKey` parameter.
   *
   * Example usage:
   * <pre>{@code
   * User exampleUser = new User();
   * exampleUser.setActive(true);
   * try {
   *   Map<Long, User> activeUsersById = entityManager.find(exampleUser, "id");
   *   activeUsersById.forEach((id, user) -> System.out.println("User ID: " + id + ", User: " + user));
   * }
   * catch (DataAccessException e) {
   *   System.err.println("Error while fetching active users: " + e.getMessage());
   * }
   * }</pre>
   *
   * @param <K> the type of keys in the resulting map, derived from the property specified by `mapKey`
   * @param <T> the type of entities being searched
   * @param example an instance of T used as a template to filter the search results;
   * non-null fields in this object are used as criteria for matching
   * @param mapKey the name of the property in the entity T to be used as the key in the resulting map
   * @return a map where each key is derived from the `mapKey` property of the matched entities,
   * and each value is the corresponding entity
   * @throws DataAccessException if there is an issue accessing or querying the data source
   */
  <K, T> Map<K, T> find(T example, String mapKey) throws DataAccessException;

  /**
   * Finds and retrieves a map of entities based on the provided example and maps them using the specified key.
   * This method is typically used for querying data from a database where the result needs to be mapped
   * to a specific key attribute.
   *
   * Example usage:
   * <pre>
   * {@code
   * // Assuming User is an entity class with fields 'id' and 'name'
   * User exampleUser = new User();
   * exampleUser.setName("John");
   *
   * Map<Long, User> usersById = entityManager.find(User.class, exampleUser, "id");
   * // The above will return a map where the keys are user IDs and the values are User objects with name "John"
   * }
   * </pre>
   *
   * @param <K> the type of the key in the resulting map
   * @param <T> the type of the entity being queried
   * @param entityClass the class of the entity to query (e.g., User.class, Product.class)
   * @param example an example object used as a template for the query; non-null fields are used as criteria
   * @param mapKey the name of the property or field to use as the key in the resulting map
   * @return a map where the keys are derived from the specified mapKey and the values are the matching entities
   * @throws DataAccessException if there is any issue accessing the data source during the query
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Object example, String mapKey)
          throws DataAccessException;

  /**
   * Finds entities of the specified class and maps them into a {@code Map} using the provided key.
   * This method allows for custom query handling through the {@code QueryStatement} parameter.
   * If the {@code handler} is null, the default query behavior is applied.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Define a custom query handler
   *   QueryStatement queryHandler = query -> query.where("status").eq("active");
   *
   *   // Find all active users and map them by their email addresses
   *   try {
   *     Map<String, User> userMap = entityManager.find(User.class, queryHandler, "email");
   *     userMap.forEach((email, user) -> {
   *       System.out.println("User with email " + email + ": " + user.getName());
   *     });
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error occurred while fetching data: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <K> the type of keys in the resulting map
   * @param <T> the type of entities to be queried
   * @param entityClass the class of the entities to be queried (e.g., {@code User.class})
   * @param handler the query handler to customize the query; can be {@code null}
   * @param mapKey the property name to use as the key in the resulting map
   * @return a map where the keys are determined by the {@code mapKey} property
   * and the values are the corresponding entities
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, String mapKey)
          throws DataAccessException;

  /**
   * Finds and retrieves a map of entities from the data source based on the
   * provided entity class and key mapping function. The method applies the
   * key mapping function to each retrieved entity to generate the corresponding
   * key for the map.
   *
   * <p>Example usage:
   * <pre>{@code
   *    // Assuming a User entity with an 'id' field
   *    Map<Long, User> userMap = entityManager.find(User.class, User::getId);
   *
   *    // The resulting map will have the user's ID as the key and the User
   *    // object as the value. For example:
   *    // {
   *    //   1 -> User{id=1, name="Alice"},
   *    //   2 -> User{id=2, name="Bob"}
   *    // }
   * }</pre>
   *
   * @param <K> the type of the keys in the resulting map
   * @param <T> the type of the entities to be retrieved
   * @param entityClass the class of the entities to retrieve from the data source
   * @param keyMapper a function that extracts the key from an entity instance
   * @return a map where each key is generated by applying the
   * keyMapper function to an entity, and the value is the
   * corresponding entity itself
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Function<T, K> keyMapper)
          throws DataAccessException;

  /**
   * Searches for elements matching the given example and maps them to a {@link Map} using
   * the provided keyMapper function.
   * <p>
   * This method is useful when you need to retrieve multiple records based on an
   * example object and organize the results in a key-value structure. The keys are
   * generated by applying the keyMapper function to each matching element.
   *
   * <p>Example usage:
   * <pre>{@code
   *   String example = "example";
   *   Function<String, Integer> keyMapper = String::length;
   *
   *   try {
   *     Map<Integer, String> result = entityManager.find(example, keyMapper);
   *     result.forEach((key, value) -> {
   *       System.out.println("Key: " + key + ", Value: " + value);
   *     });
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error occurred while finding elements: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param example the example object used to find matching elements; must not be null
   * @param keyMapper a function that extracts the key from each matching element; must not be null
   * @param <K> the type of the keys in the resulting map
   * @param <T> the type of the elements being searched and stored in the map
   * @return a {@link Map} containing the matched elements, where the keys are generated
   * by the keyMapper function and the values are the matched elements
   * @throws DataAccessException if an error occurs during data access or retrieval
   */
  <K, T> Map<K, T> find(T example, Function<T, K> keyMapper) throws DataAccessException;

  /**
   * Searches for entities of the specified type that match the given example
   * and maps the results to a {@code Map} using the provided key mapper function.
   * <p>
   * This method is typically used to retrieve a collection of entities from a
   * data store, where each entity is associated with a unique key generated by
   * the {@code keyMapper}. The search criteria are defined by the {@code example}
   * object, which may represent a query or filter condition.
   * <p>
   * Example usage:
   * <pre>{@code
   *   // Define an example object for filtering
   *   User exampleUser = new User();
   *   exampleUser.setStatus("ACTIVE");
   *
   *   // Find users and map them by their user ID
   *   Map<Long, User> userMap = entityManager.find(User.class, exampleUser, User::getId);
   *
   *   // Iterate over the result map
   *   userMap.forEach((id, user) -> {
   *     System.out.println("User ID: " + id + ", User Name: " + user.getName());
   *   });
   * }</pre>
   *
   * @param <K> the type of keys in the resulting map
   * @param <T> the type of entities to search for
   * @param entityClass the class of the entity type to search for; must not be null
   * @param example an object representing the search criteria or filter;
   * can be null if no specific criteria are required
   * @param keyMapper a function to extract the key from each entity; must not be null
   * @return a map containing the found entities, where keys are
   * generated by the {@code keyMapper} and values are the
   * corresponding entities
   * @throws DataAccessException if there is any issue accessing the underlying
   * data store during the search operation
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Object example, Function<T, K> keyMapper)
          throws DataAccessException;

  /**
   * Finds and retrieves a map of entities based on the provided entity class,
   * query handler, and key mapping function. This method is useful for fetching
   * data from a data source and organizing it into a map structure.
   *
   * <p><b>Usage Example:</b></p>
   *
   * Suppose you have an entity class {@code User} and want to fetch users
   * indexed by their user IDs:
   * <pre>{@code
   *   QueryStatement query = QueryBuilder.select().from("users").where("active", true);
   *   Map<Long, User> userMap = entityManager.find(User.class, query, User::getId);
   *
   *   // Accessing a specific user by ID
   *   User user = userMap.get(123L);
   * }</pre>
   *
   * @param <K> the type of keys in the resulting map
   * @param <T> the type of entities to be retrieved
   * @param entityClass the class of the entities to query (e.g., {@code User.class})
   * @param handler the query statement to execute; can be {@code null} if no
   * specific query conditions are required
   * @param keyMapper a function to extract the key from each entity (e.g., {@code User::getId})
   * @return a map where keys are derived using the {@code keyMapper}
   * and values are the retrieved entities
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, Function<T, K> keyMapper)
          throws DataAccessException;

  /**
   * Counts the number of elements that match the given example object.
   *
   * <p>This method is typically used in data access scenarios where you need to determine
   * how many records or entities match a specific example. The example object serves as
   * a template, and fields with non-null values are used as criteria for counting.
   *
   * <p>Example usage:
   * <pre>{@code
   *   User userExample = new User();
   *   userExample.setStatus("active");
   *
   *   try {
   *     Number count = entityManager.count(userExample);
   *     System.out.println("Number of active users: " + count);
   *   }
   *   catch (DataAccessException e) {
   *     System.err.println("Error while counting users: " + e.getMessage());
   *   }
   * }</pre>
   *
   * @param <T> the type of the example object
   * @param example the example object used as a template for matching;
   * non-null fields are treated as criteria
   * @return the count of matching elements, represented as a {@link Number}
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> Number count(T example) throws DataAccessException;

  /**
   * Counts the total number of entities of the specified class in the data store.
   * This method is useful for determining the size of a dataset without retrieving
   * all the entities, which can be more efficient for large datasets.
   *
   * Example usage:
   * <pre>{@code
   *   long userCount = entityManager.count(User.class);
   *   System.out.println("Total users: " + userCount);
   * }</pre>
   *
   * In the example above, the {@code count} method is used to retrieve the total
   * number of {@code User} entities stored in the data store.
   *
   * @param <T> the type of the entity class
   * @param entityClass the class of the entities to count; must not be null
   * @return the total number of entities of the specified class as a Number
   * @throws DataAccessException if there is an issue accessing the data store
   */
  <T> Number count(Class<T> entityClass) throws DataAccessException;

  /**
   * Counts the number of entities of the specified class that match the given example.
   * This method is typically used to determine the size of a dataset that satisfies
   * certain criteria defined by the example object.
   *
   * <p>Example usage:
   * <pre>{@code
   * // Assuming User is an entity class and we want to count users with a specific name
   * User exampleUser = new User();
   * exampleUser.setName("John");
   *
   * long userCount = entityManager.count(User.class, exampleUser).longValue();
   * System.out.println("Number of users named John: " + userCount);
   * }</pre>
   *
   * @param <T> the type of the entity class
   * @param entityClass the class of the entity for which the count is to be performed
   * @param example an instance of the entity class or a map containing the fields
   * and values to match; null if no filtering is required
   * @return the count of matching entities, typically returned as a Number
   * subclass such as Long or Integer
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <T> Number count(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * Counts the number of entities of the specified class that match the given condition.
   * This method is typically used to retrieve the total count of records in a database
   * table that satisfy specific criteria.
   *
   * <p>Example usage:
   * <pre>{@code
   * ConditionStatement condition = Condition.eq("status", "active");
   * long activeUserCount = entityManager.count(User.class, condition).longValue();
   * System.out.println("Active users: " + activeUserCount);
   * }</pre>
   *
   * <p>If no condition is provided, the method will return the total count of all
   * entities of the specified class:
   * <pre>{@code
   * long totalUsers = entityManager.count(User.class, null).longValue();
   * System.out.println("Total users: " + totalUsers);
   * }</pre>
   *
   * @param <T> the type of the entity class
   * @param entityClass the class of the entity for which the count is to be calculated;
   * must not be null
   * @param handler the condition statement specifying the criteria for counting;
   * can be null if no specific condition is required
   * @return the count of entities matching the condition as a Number object;
   * the exact runtime type (e.g., Integer, Long) depends on the
   * underlying implementation
   * @throws DataAccessException if there is any issue accessing the data source
   */
  <T> Number count(Class<T> entityClass, @Nullable ConditionStatement handler)
          throws DataAccessException;

  /**
   * Retrieves a paginated result set of entities of the specified type.
   * This method queries the underlying data store and returns a {@link Page}
   * object containing the requested data. If no pagination parameters are
   * provided, it may return all available data as a single page.
   *
   * <p><b>Usage Example:</b></p>
   *
   * Assuming you have an entity class {@code User} and want to retrieve
   * the first page of users with a page size of 10:
   * <pre>{@code
   * Pageable pageable = Pageable.of(1, 10);
   * Page<User> userPage = entityManager.page(User.class, pageable);
   *
   * // Iterate over the content of the page
   * for (User user : userPage.getRows()) {
   *   System.out.println(user.getName());
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to retrieve
   * @param entityClass the class object representing the entity type
   * to query (e.g., {@code User.class})
   * @param pageable the pagination information, including page number,
   * page size, and sorting options. Can be {@code null}
   * if no pagination is required
   * @return a {@link Page} object containing the paginated result set
   * @throws DataAccessException if there is an issue accessing the data store
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable Pageable pageable)
          throws DataAccessException;

  /**
   * Queries a paginated result set based on the provided example object.
   * This method is typically used for filtering and retrieving data in pages,
   * which is useful for large datasets to improve performance and usability.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Create an example object with desired filtering criteria
   *   User exampleUser = new User();
   *   exampleUser.setStatus("ACTIVE");
   *
   *   try {
   *     Page<User> userPage = entityManager.page(exampleUser);
   *
   *     // Process the paginated results
   *     List<User> users = userPage.getItems();
   *     long totalItems = userPage.getTotalRows();
   *     int totalPages = userPage.getTotalPages();
   *
   *     System.out.println("Total Users: " + totalItems);
   *     users.forEach(System.out::println);
   *   } catch (DataAccessException e) {
   *     // Handle data access exception
   *     e.printStackTrace();
   *   }
   * }</pre>
   *
   * @param <T> the type of the example object and the resulting page items
   * @param example an instance of T representing the filter criteria;
   * non-null properties of this object are used to construct the query
   * @return a {@link Page} object containing the filtered results,
   * including the list of items, total count, and pagination metadata
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> Page<T> page(T example) throws DataAccessException;

  /**
   * Queries the database and returns a paginated result based on the provided example object
   * and pagination parameters. This method is typically used for implementing query-by-example
   * functionality with support for pagination.
   *
   * <p>Example usage:
   * <pre>{@code
   * User userExample = new User();
   * userExample.setStatus("active");
   * Pageable pageable = Pageable.of(0, 10);
   *
   * try {
   *   Page<User> result = entityManager.page(userExample, pageable);
   *   List<User> users = result.getRows();
   *   long totalElements = result.getTotalRows();
   * } catch (DataAccessException e) {
   *   // Handle data access exception
   * }
   * }</pre>
   *
   * @param <T> the type of the entity being queried
   * @param example the example object used to define query criteria. Non-null fields in this
   * object will be used to filter the results. Must not be null.
   * @param pageable the pagination and sorting configuration. If null, no pagination or sorting
   * will be applied, and all matching records will be returned as a single page.
   * @return a {@link Page} object containing the query results, including the content
   * of the current page and metadata such as total elements and total pages.
   * @throws DataAccessException if there is an issue accessing the underlying database
   */
  <T> Page<T> page(T example, @Nullable Pageable pageable) throws DataAccessException;

  /**
   * Queries a paginated result set based on the provided entity class and example object.
   * This method is typically used for retrieving data in pages, which is useful for handling
   * large datasets efficiently.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Define an example object with query criteria
   *   UserExample example = new UserExample();
   *   example.createCriteria().andNameLike("%John%");
   *
   *   // Query the paginated result
   *   Page<User> userPage = entityManager.page(User.class, example);
   *
   *   // Iterate through the results
   *   for (User user : userPage.getRows()) {
   *     System.out.println(user.getName());
   *   }
   * }</pre>
   *
   * @param <T> the type of entities to be queried
   * @param entityClass the class of the entity to be queried. This class is used
   * to determine the table and structure of the data.
   * @param example an object containing the query criteria. This can be a
   * dynamic query object or a custom example instance.
   * @return a {@link Page} object containing the paginated results. The returned
   * object includes both the data records and pagination metadata such as
   * total count and current page number.
   * @throws DataAccessException if there is an issue accessing the data layer,
   * such as database connectivity problems or query errors.
   */
  <T> Page<T> page(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * Queries a paginated result set based on the provided entity class, example object,
   * and optional pagination parameters. This method is typically used for filtering
   * and retrieving data in a structured format.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Define the entity class and example object
   *   Class<User> userClass = User.class;
   *   User exampleUser = new User();
   *   exampleUser.setStatus("ACTIVE");
   *
   *   // Create pagination configuration
   *   Pageable pageable = Pageable.of(1, 10);
   *
   *   // Perform the paginated query
   *   Page<User> userPage = entityManager.page(userClass, exampleUser, pageable);
   *
   *   // Process the results
   *   userPage.peek(user -> {
   *     System.out.println("User ID: " + user.getId());
   *   });
   * }</pre>
   *
   * @param <T> the type of the entity to be queried
   * @param entityClass the class of the entity to be queried; must not be null
   * @param example the example object used for filtering; properties of this
   * object are used to construct the query conditions
   * @param pageable the pagination configuration, including page number, size,
   * and sorting options; can be null if pagination is not required
   * @return a {@link Page} object containing the query results, including the content
   * of the current page and pagination metadata
   * @throws DataAccessException if there is any issue accessing the data layer during
   * the query execution
   */
  <T> Page<T> page(Class<T> entityClass, Object example, @Nullable Pageable pageable)
          throws DataAccessException;

  /**
   * Queries a paginated result set for the specified entity class using an optional condition handler.
   * This method is typically used to fetch data in pages, which is useful for handling large datasets efficiently.
   *
   * Example usage:
   * <pre>{@code
   *   ConditionStatement condition = query -> query.eq("status", "ACTIVE");
   *   Page<User> userPage = entityManager.page(User.class, condition);
   *   userPage.getRows().forEach(user -> {
   *     System.out.println(user.getName());
   *   });
   * }</pre>
   *
   * If no condition handler is provided, all records for the given entity class will be considered.
   * Ensure proper exception handling for potential data access issues.
   *
   * @param <T> the type of the entity class being queried
   * @param entityClass the class object representing the entity type to query (e.g., User.class)
   * @param handler an optional condition handler to filter or customize the query; can be null
   * @return a {@link Page} object containing the paginated results for the specified entity class
   * @throws DataAccessException if there is any issue accessing the underlying data source
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler)
          throws DataAccessException;

  /**
   * Queries a paginated result set for the specified entity class using optional
   * condition statements and pagination information.
   *
   * <p>This method allows retrieving a subset of data from a database in a paginated
   * manner. The {@code entityClass} parameter specifies the type of entities to query,
   * while the {@code handler} parameter can be used to define custom filtering or
   * conditions. Pagination details like page number and size can be provided via the
   * {@code pageable} parameter.</p>
   *
   * <p><b>Example Usage:</b></p>
   * <pre>{@code
   *   // Define a condition statement
   *   ConditionStatement condition = query -> query.eq("status", "ACTIVE");
   *
   *   // Create pagination details
   *   Pageable pageable = Pageable.of(1, 10); // Fetch the first 10 records
   *
   *   // Query the paginated result
   *   Page<User> userPage = entityManager.page(User.class, condition, pageable);
   *
   *   // Iterate over the result
   *   userPage.peek(user -> {
   *     System.out.println(user.getName());
   *   });
   * }</pre>
   *
   * @param <T> the type of the entity to query
   * @param entityClass the class object of the entity type (e.g., {@code User.class})
   * @param handler an optional condition statement to filter the query results;
   * pass {@code null} if no filtering is required
   * @param pageable an optional pagination configuration; pass {@code null} if
   * pagination is not needed
   * @return a {@link Page} object containing the queried entities along with pagination
   * metadata such as total elements and total pages
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler, @Nullable Pageable pageable)
          throws DataAccessException;

  /**
   * Iterates over a collection of entities matching the provided example and
   * applies the given consumer function to each entity. This method is useful
   * for processing large datasets in a streaming manner without loading all
   * entities into memory at once.
   *
   * <p>Example usage:
   * <pre>{@code
   * ExampleEntity example = new ExampleEntity();
   * example.setStatus("ACTIVE");
   *
   * entityManager.iterate(example, entity -> {
   *   System.out.println("Processing entity: " + entity);
   *   // Perform additional operations on the entity
   * });
   * }</pre>
   *
   * @param <T> the type of the entities to iterate over
   * @param example an instance of T used as a template to find matching
   * entities; non-null properties of the example are used
   * as criteria for filtering
   * @param entityConsumer a Consumer function that processes each entity; this
   * function is applied to every matching entity found
   * @throws DataAccessException if there is an issue accessing the data source
   * during iteration
   */
  <T> void iterate(T example, Consumer<T> entityConsumer) throws DataAccessException;

  /**
   * Iterates over entities of the specified class that match the given example
   * and applies the provided consumer function to each entity.
   *
   * <p>This method is typically used for processing large datasets in a streaming
   * manner, avoiding the need to load all matching entities into memory at once.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyEntity example = new MyEntity();
   *   example.setStatus("ACTIVE");
   *
   *   entityManager.iterate(MyEntity.class, example, entity -> {
   *     System.out.println("Processing entity: " + entity.getId());
   *     // Perform additional operations on the entity
   *   });
   * }</pre>
   *
   * @param <T> the type of the entity class
   * @param entityClass the class object representing the entity type to query
   * @param example an instance of the entity class used as a query example;
   * non-null fields are used as criteria for matching
   * @param entityConsumer a consumer function to process each matched entity;
   * should not modify the entity unless explicitly intended
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <T> void iterate(Class<T> entityClass, Object example, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * Iterates over all entities of the specified class and applies the provided
   * consumer logic to each entity. This method allows for optional query
   * customization through the {@code handler} parameter.
   *
   * <p>The iteration process retrieves entities of type {@code T} from the data
   * source and passes each one to the {@code entityConsumer} for processing.
   * If a {@code handler} is provided, it can be used to define additional query
   * constraints or sorting rules.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * // Iterate over all User entities and print their names
   * entityManager.iterate(User.class, null, user -> {
   *   System.out.println("User: " + user.getName());
   * });
   *
   * // Iterate with a custom query to filter active users
   * QueryStatement query = query -> query.where("active", true);
   * entityManager.iterate(User.class, query, user -> {
   *   System.out.println("Active User: " + user.getName());
   * });
   * }</pre>
   *
   * @param <T> the type of the entity being iterated
   * @param entityClass the class object representing the entity type to iterate over
   * @param handler an optional query statement to customize the retrieval
   * process (e.g., filtering or sorting); can be {@code null}
   * @param entityConsumer a consumer function that processes each retrieved entity
   * @throws DataAccessException if an error occurs during data access or iteration
   */
  <T> void iterate(Class<T> entityClass, @Nullable QueryStatement handler, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * Returns an {@code EntityIterator} for iterating over entities of the specified class.
   * This method is typically used to efficiently traverse large datasets without loading
   * all entities into memory at once.
   *
   * <p>Example usage:
   * <pre>{@code
   *   try {
   *     EntityIterator<MyEntity> iterator = entityManager.iterate(MyEntity.class);
   *     while (iterator.hasNext()) {
   *       MyEntity entity = iterator.next();
   *       // Process the entity
   *       System.out.println(entity);
   *     }
   *   }
   *   catch (DataAccessException e) {
   *     // Handle data access exception
   *     e.printStackTrace();
   *   }
   * }</pre>
   *
   * @param <T> the type of entities to iterate over
   * @param entityClass the class object representing the type of entities to iterate over
   * @return an {@code EntityIterator} instance for the specified entity class
   * @throws DataAccessException if there is an issue accessing the underlying data source
   */
  <T> EntityIterator<T> iterate(Class<T> entityClass) throws DataAccessException;

  /**
   * Iterates over entities in the data store that match the given example object.
   * This method is typically used for querying multiple records based on a prototype
   * entity. The iteration allows for efficient processing of large datasets without
   * loading all entities into memory at once.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyEntity example = new MyEntity();
   *   example.setStatus("ACTIVE");
   *
   *   try (EntityIterator<MyEntity> iterator = entityManager.iterate(example)) {
   *     while (iterator.hasNext()) {
   *       MyEntity entity = iterator.next();
   *       System.out.println(entity.getId());
   *     }
   *   }
   *   catch (DataAccessException e) {
   *     e.printStackTrace();
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity being iterated
   * @param example an instance of T representing the prototype for matching entities;
   * non-null fields in this object are used as query criteria
   * @return an {@link EntityIterator} that allows iterating over matching entities
   * @throws DataAccessException if there is an issue accessing the underlying data store
   */
  <T> EntityIterator<T> iterate(T example) throws DataAccessException;

  /**
   * Iterates over entities of the specified class that match the given example.
   * This method provides a way to lazily retrieve entities from the data store,
   * which is useful for processing large datasets without loading all records
   * into memory at once.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyClass example = new MyClass();
   *   example.setStatus("active");
   *
   *   try (EntityIterator<MyClass> iterator = entityManager.iterate(MyClass.class, example)) {
   *     while (iterator.hasNext()) {
   *       MyClass entity = iterator.next();
   *       System.out.println(entity);
   *     }
   *   }
   *   catch (DataAccessException e) {
   *     e.printStackTrace();
   *   }
   * }</pre>
   *
   * @param <T> the type of the entity class to iterate over
   * @param entityClass the class object representing the entity type to query
   * @param example an instance of the entity class used as an example for
   * filtering results. Non-null fields in the example are
   * used as criteria for matching.
   * @return an {@link EntityIterator} that allows iterating over the matched
   * entities. The iterator must be closed after use to release resources.
   * @throws DataAccessException if there is an issue accessing the data store
   */
  <T> EntityIterator<T> iterate(Class<T> entityClass, Object example)
          throws DataAccessException;

  /**
   * Iterates over entities of the specified class using an optional query handler.
   * This method provides a way to traverse through entities in a structured manner,
   * allowing for custom query logic if needed.
   *
   * <p>Example usage:
   * <pre>{@code
   * EntityIterator<MyEntity> iterator = entityManager.iterate(MyEntity.class, null);
   * while (iterator.hasNext()) {
   *   MyEntity entity = iterator.next();
   *   // Process the entity
   * }
   * }</pre>
   *
   * <p>For more advanced queries, you can provide a {@link QueryStatement}:
   * <pre>{@code
   * var query = new Query("SELECT * FROM my_table WHERE status = ?", "active");
   * EntityIterator<MyEntity> iterator = entityManager.iterate(MyEntity.class, query);
   * while (iterator.hasNext()) {
   *   MyEntity entity = iterator.next();
   *   // Handle filtered entities
   * }
   * }</pre>
   *
   * @param <T> the type of the entity to iterate over
   * @param entityClass the class of the entity to be iterated; must not be null
   * @param handler an optional query handler to customize the iteration logic;
   * pass null if no specific query is required
   * @return an {@link EntityIterator} instance that allows iterating
   * over the entities of the specified class
   * @throws DataAccessException if there is an issue accessing the data source
   */
  <T> EntityIterator<T> iterate(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

}
