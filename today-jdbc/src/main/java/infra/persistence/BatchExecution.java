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

import java.util.ArrayList;

/**
 * Represents a batch execution context for executing bulk database operations.
 *
 * <p>This class encapsulates the necessary information and configuration for performing
 * batch operations on a database. It includes the SQL statement to execute, metadata about
 * the entity being operated on, and the strategy for updating properties during the batch.
 *
 * <p>Instances of this class are immutable except for the {@code entities} list, which is
 * used to collect entities to be processed in the batch. The class is typically used in
 * conjunction with a {@code PreparedBatch} to manage and execute batch operations.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Define the SQL statement and metadata
 * String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
 * EntityMetadata metadata = EntityMetadataFactory.create(User.class);
 * PropertyUpdateStrategy strategy = PropertyUpdateStrategy.DEFAULT;
 *
 * // Create a BatchExecution instance
 * BatchExecution batchExecution = new BatchExecution(sql, strategy, metadata, true);
 *
 * // Add entities to the batch
 * batchExecution.entities.add(new User("Alice", 30));
 * batchExecution.entities.add(new User("Bob", 25));
 *
 * // Execute the batch using PreparedBatch
 * try (Connection connection = dataSource.getConnection()) {
 *   PreparedBatch preparedBatch = new PreparedBatch(connection, sql, strategy, metadata, properties, true);
 *   for (Object entity : batchExecution.entities) {
 *     preparedBatch.addBatchUpdate(entity, 100);
 *   }
 *   preparedBatch.explicitExecuteBatch();
 * } catch (SQLException | Throwable e) {
 *   e.printStackTrace();
 * }
 * }</pre>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Encapsulates SQL statement and entity metadata for batch operations.</li>
 *   <li>Supports auto-generated IDs for entities.</li>
 *   <li>Provides flexibility through the {@code PropertyUpdateStrategy} for property updates.</li>
 *   <li>Collects entities to be processed in the batch via the {@code entities} list.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityMetadata
 * @see PropertyUpdateStrategy
 * @since 4.0 2024/2/20 23:25
 */
public class BatchExecution {

  public final String sql;

  public final boolean autoGenerateId;

  public final EntityMetadata entityMetadata;

  public final PropertyUpdateStrategy strategy;

  public final ArrayList<Object> entities = new ArrayList<>();

  BatchExecution(String sql, PropertyUpdateStrategy strategy,
          EntityMetadata entityMetadata, boolean autoGenerateId) {
    this.sql = sql;
    this.strategy = strategy;
    this.entityMetadata = entityMetadata;
    this.autoGenerateId = autoGenerateId;
  }

}
