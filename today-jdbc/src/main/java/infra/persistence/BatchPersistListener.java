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

import infra.lang.Nullable;

/**
 * A listener interface for monitoring batch persistence operations.
 * Implementations can be used to execute custom logic before and after
 * batch processing occurs.
 *
 * <p>This interface provides two callback methods:
 * <ul>
 *   <li>{@link #beforeProcessing(BatchExecution, boolean)}: Invoked before batch processing begins.</li>
 *   <li>{@link #afterProcessing(BatchExecution, boolean, Throwable)}: Invoked after batch processing completes or fails.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class LoggingBatchPersistListener implements BatchPersistListener {
 *
 *   @Override
 *   public void beforeProcessing(BatchExecution execution, boolean implicitExecution) {
 *     System.out.println("Starting batch processing for SQL: " + execution.sql);
 *     System.out.println("Number of entities in batch: " + execution.entities.size());
 *   }
 *
 *   @Override
 *   public void afterProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
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
 *   listener.beforeProcessing(execution, false);
 *   // Perform batch operations here
 *   listener.afterProcessing(execution, false, null);
 * }
 * catch (Exception e) {
 *   listener.afterProcessing(execution, false, e);
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
   *   public void beforeProcessing(BatchExecution execution, boolean implicitExecution) {
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
   * // Trigger the beforeProcessing callback
   * listener.beforeProcessing(execution, false);
   * }</pre>
   *
   * @param execution the batch execution metadata, including the SQL statement,
   * entities, and other relevant details
   * @param implicitExecution a flag indicating whether the batch execution
   * is implicit (e.g., triggered automatically by the system)
   */
  default void beforeProcessing(BatchExecution execution, boolean implicitExecution) {
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
   *   public void afterProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
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
   * listener.afterProcessing(execution, false, null);
   *
   * // Simulate batch processing with an exception
   * listener.afterProcessing(execution, true, new RuntimeException("Database error"));
   * }</pre>
   *
   * @param execution the batch execution metadata, including the SQL statement,
   * entities, and other relevant details
   * @param implicitExecution a flag indicating whether the batch execution
   * was implicit (e.g., triggered automatically by the system)
   * @param exception the exception that occurred during batch processing, if any;
   * {@code null} if no exception occurred
   */
  void afterProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception);

}
