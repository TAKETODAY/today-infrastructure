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

import org.jspecify.annotations.Nullable;

/**
 * Listener for observing the lifecycle of batch persistence operations.
 *
 * <p>Implementations are notified before a batch is executed and again after it
 * completes, either successfully or with an exception. A single batch is executed
 * implicitly once the configured batch size is reached, or explicitly when the
 * pending batch is flushed.
 *
 * <p>Entity pre/post callbacks still participate in batch operations, but entity
 * failure callbacks are not invoked. Failures during execution (including
 * generated ID assignment and entity post callbacks) are reported here.
 * Preparation failures before execution begins are propagated to the caller
 * without a batch execution notification.
 *
 * <p>Batch completion is not transaction completion: an earlier successful
 * batch may subsequently be rolled back. Use
 * {@link infra.transaction.support.TransactionSynchronization#afterCommit()}
 * or {@link infra.transaction.support.TransactionSynchronization#afterCompletion(int)}
 * to observe the final transaction outcome, including an enclosing transaction.
 *
 * <p>The callback methods are:
 * <ul>
 *   <li>{@link #preProcessing(BatchExecution, boolean)}: invoked before batch
 *       execution begins.</li>
 *   <li>{@link #postProcessing(BatchExecution, boolean, Throwable)}: invoked after
 *       batch execution completes or fails.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class LoggingBatchExecutionListener implements BatchExecutionListener {
 *
 *   @Override
 *   public void preProcessing(BatchExecution execution, boolean implicitExecution) {
 *     System.out.println("Executing " + execution.entities.size()
 *         + " entities with SQL: " + execution.statement);
 *   }
 *
 *   @Override
 *   public void postProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
 *     if (exception != null) {
 *       System.err.println("Batch execution failed: " + exception.getMessage());
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Listeners are registered through the entity manager and can be ordered using
 * {@link infra.core.annotation.Order @Order} or {@link infra.core.Ordered}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.core.annotation.Order
 * @see infra.core.Ordered
 * @since 4.0 2022/9/20 12:47
 */
public interface BatchExecutionListener extends Listener {

  /**
   * Called before a batch is executed.
   *
   * @param execution the batch execution metadata, including the SQL statement and
   * the entities collected for the batch
   * @param implicitExecution {@code true} if the execution was triggered implicitly
   * because the configured batch size was reached; {@code false} if the pending
   * batch was flushed explicitly
   */
  default void preProcessing(BatchExecution execution, boolean implicitExecution) {
  }

  /**
   * Called after a batch execution has completed, whether successfully or with an
   * exception. All registered post-processing listeners are invoked even if
   * a listener throws. Listener exceptions are suppressed on the execution
   * failure, or on the first listener failure when execution succeeded. In
   * the latter case that first failure is propagated after all listeners run.
   * Each listener receives the original execution exception (or {@code null}).
   *
   * @param execution the batch execution metadata, including the SQL statement and
   * the entities collected for the batch
   * @param implicitExecution {@code true} if the execution was triggered implicitly
   * because the configured batch size was reached; {@code false} if the pending
   * batch was flushed explicitly
   * @param exception the exception thrown during batch processing, or {@code null}
   * if the batch completed successfully
   */
  void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception);

}
