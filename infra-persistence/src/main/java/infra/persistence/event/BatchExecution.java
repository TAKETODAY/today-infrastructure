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
import java.util.List;

import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;

/**
 * Holds the execution context for batch save and batch update operations.
 *
 * <p>Both operations share this context: {@link #statement} identifies the SQL to
 * execute, {@link #entityMetadata} describes the entity type, and
 * {@link #properties} exposes the selected properties in SQL parameter order.
 * {@link #autoGenerateId} indicates whether auto-generated IDs are handled.
 * Use {@link #getOperation()} to determine the operation type.
 *
 * <p>The metadata references are fixed for the lifetime of the context, and the
 * selected property list is immutable. The mutable {@link #entities} list collects
 * the entities to be saved or updated. {@link BatchExecutionListener} callbacks
 * receive this context before and after batch execution to inspect the SQL,
 * entities, and cumulative {@linkplain #getAffectedRows() affected row count}.
 *
 * <p>Instances are created by the persistence infrastructure and are not intended
 * to be constructed by application code.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BatchExecutionListener
 * @see EntityMetadata
 * @see EntityProperty
 * @since 4.0 2024/2/20 23:25
 */
public abstract class BatchExecution {

  /**
   * The SQL statement used for the batch save or update operation.
   */
  public final String statement;

  /**
   * Whether auto-generated IDs are handled for this batch.
   */
  public final boolean autoGenerateId;

  /**
   * The metadata describing the entities saved or updated by this batch.
   */
  public final EntityMetadata entityMetadata;

  /**
   * The immutable selected property list, in SQL parameter order, shared by
   * every entity in this batch.
   */
  public final List<EntityProperty> properties;

  /**
   * The mutable list of entities collected for batch saving or updating.
   */
  public final ArrayList<Object> entities = new ArrayList<>();

  /**
   * Create an execution context for a batch save or update operation.
   *
   * @param statement the SQL statement to execute
   * @param entityMetadata the metadata describing the entity type
   * @param properties the selected properties in SQL parameter order; copied to
   * an immutable list
   * @param autoGenerateId whether auto-generated IDs are handled for this batch
   */
  protected BatchExecution(String statement, EntityMetadata entityMetadata,
          List<EntityProperty> properties, boolean autoGenerateId) {
    this.statement = statement;
    this.properties = List.copyOf(properties);
    this.entityMetadata = entityMetadata;
    this.autoGenerateId = autoGenerateId;
  }

  /**
   * Return the type of persistence operation performed by this batch.
   *
   * @return the batch operation type
   * @since 5.0
   */
  public abstract BatchOperation getOperation();

  /**
   * Return whether this context represents a batch insert operation.
   *
   * @return {@code true} if the operation is {@link BatchOperation#INSERT}
   * @since 5.0
   */
  public final boolean isInsert() {
    return getOperation() == BatchOperation.INSERT;
  }

  /**
   * Return whether this context represents a batch update operation.
   *
   * @return {@code true} if the operation is {@link BatchOperation#UPDATE}
   * @since 5.0
   */
  public final boolean isUpdate() {
    return getOperation() == BatchOperation.UPDATE;
  }

  /**
   * Return whether this context represents a batch delete operation.
   *
   * @return {@code true} if the operation is {@link BatchOperation#DELETE}
   * @since 5.0
   */
  public final boolean isDelete() {
    return getOperation() == BatchOperation.DELETE;
  }

  /**
   * Return the total number of rows affected by this batch execution so far.
   *
   * <p>The count accumulates across every execution of the same batch, whether
   * triggered implicitly when the configured batch size is reached or explicitly
   * when the pending batch is flushed. It is available to
   * {@link BatchExecutionListener} callbacks.
   *
   * @return the total number of rows affected by the batch execution
   */
  public abstract int getAffectedRows();

}
