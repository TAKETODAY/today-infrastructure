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

import org.jspecify.annotations.Nullable;

/**
 * A listener interface for monitoring batch persistence operations.
 * Implementations can be used to execute custom logic before and after
 * batch processing occurs.
 *
 * <p>This interface provides two callback methods:
 * <ul>
 *   <li>{@link #preProcessing(BatchExecution, boolean)}: Invoked before batch processing begins.</li>
 *   <li>{@link #postProcessing(BatchExecution, boolean, Throwable)}: Invoked after batch processing completes or fails.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class LoggingBatchPersistListener implements BatchPersistListener {
 *
 *   @Override
 *   public void preProcessing(BatchExecution execution, boolean implicitExecution) {
 *     System.out.println("Starting batch processing for SQL: " + execution.sql);
 *     System.out.println("Number of entities in batch: " + execution.entities.size());
 *   }
 *
 *   @Override
 *   public void postProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
 *     if (exception != null) {
 *       System.err.println("Batch processing failed with error: " + exception.getMessage());
 *     } else {
 *       System.out.println("Batch processing completed successfully.");
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Integration Example:</strong>
 * <pre>{@code
 * BatchPersistListener listener = new LoggingBatchPersistListener();
 * BatchExecution execution = new BatchExecution("INSERT INTO users (name) VALUES (?)", null, null, false);
 *
 * try {
 *   listener.preProcessing(execution, false);
 *   // Perform batch operations here
 *   listener.postProcessing(execution, false, null);
 * }
 * catch (Exception e) {
 *   listener.postProcessing(execution, false, e);
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/20 12:47
 */
public interface BatchPersistListener {

  /**
   * Invoked before batch processing begins. This method allows custom logic
   * to be executed prior to the execution of batch operations. It provides
   * access to the batch execution metadata and a flag indicating whether the
   * execution is implicit.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * public class LoggingBatchPersistListener implements BatchPersistListener {
   *
   *   @Override
   *   public void preProcessing(BatchExecution execution, boolean implicitExecution) {
   *     System.out.println("Starting batch processing for SQL: " + execution.sql);
   *     System.out.println("Number of entities in batch: " + execution.entities.size());
   *     if (implicitExecution) {
   *       System.out.println("This is an implicit batch execution.");
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p><strong>Integration Example:</strong>
   * <pre>{@code
   * BatchPersistListener listener = new LoggingBatchPersistListener();
   * BatchExecution execution = new BatchExecution("INSERT INTO users (name) VALUES (?)", null, null, false);
   *
   * // Trigger the preProcessing callback
   * listener.preProcessing(execution, false);
   * }</pre>
   *
   * @param execution the batch execution metadata, including the SQL statement,
   * entities, and other relevant details
   * @param implicitExecution a flag indicating whether the batch execution
   * is implicit (e.g., triggered automatically by the system)
   */
  default void preProcessing(BatchExecution execution, boolean implicitExecution) {
  }

  /**
   * Invoked after batch processing has completed. This method allows custom logic
   * to be executed after the execution of batch operations. It provides access to
   * the batch execution metadata, a flag indicating whether the execution was implicit,
   * and any exception that may have occurred during processing.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * public class LoggingBatchPersistListener implements BatchPersistListener {
   *
   *   @Override
   *   public void postProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
   *     if (exception == null) {
   *       System.out.println("Batch processing completed successfully for SQL: " + execution.sql);
   *       System.out.println("Number of entities processed: " + execution.entities.size());
   *     } else {
   *       System.err.println("Batch processing failed with exception: " + exception.getMessage());
   *     }
   *     if (implicitExecution) {
   *       System.out.println("This was an implicit batch execution.");
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p><strong>Integration Example:</strong>
   * <pre>{@code
   * BatchPersistListener listener = new LoggingBatchPersistListener();
   * BatchExecution execution = new BatchExecution("INSERT INTO users (name) VALUES (?)", null, null, false);
   *
   * // Simulate successful batch processing
   * listener.postProcessing(execution, false, null);
   *
   * // Simulate batch processing with an exception
   * listener.postProcessing(execution, true, new RuntimeException("Database error"));
   * }</pre>
   *
   * @param execution the batch execution metadata, including the SQL statement,
   * entities, and other relevant details
   * @param implicitExecution a flag indicating whether the batch execution
   * was implicit (e.g., triggered automatically by the system)
   * @param exception the exception that occurred during batch processing, if any;
   * {@code null} if no exception occurred
   */
  void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception);

}
