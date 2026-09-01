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

package infra.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import infra.logging.LoggerFactory;

/**
 * Strategy for handling a listener notification {@link Runnable} that has been
 * rejected by an {@link Executor}: {@link Executor#execute(Runnable)} threw a
 * {@link RejectedExecutionException} (for example because the executor has been
 * shut down or exhausted its queue).
 *
 * <p>{@link Future} delegates rejected listener notification tasks to the
 * currently configured handler, which defaults to {@link #DISCARD}. The handler
 * decides whether the task is executed inline on the caller thread, silently
 * dropped, or routed elsewhere.
 *
 * <p>The handler runs on the thread that attempted the submission; it must not
 * assume any particular security context, thread state, or ordering guarantees.
 *
 * <p><strong>Exception propagation:</strong> the handler is invoked synchronously
 * from the code that completes the future ({@code setSuccess()}, {@code setFailure()},
 * or {@code cancel()}). Any exception thrown by the handler is <em>not</em> swallowed
 * by the framework: it propagates back to that completing caller. Implementations
 * must therefore catch and handle their own exceptions if they must not disturb the
 * completion path.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface RejectedExecutionHandler {

  /**
   * Runs the rejected task inline on the thread that attempted the submission,
   * after logging a warning. Guarantees that the task is never lost.
   */
  RejectedExecutionHandler CALLER_RUNS = (executor, task, cause) -> {
    LoggerFactory.getLogger(Future.class).warn("Failed to submit a task to {}; running it in the caller thread",
            executor.getClass().getName(), cause);
    task.run();
  };

  /**
   * Logs a warning and drops the rejected task without executing it.
   */
  RejectedExecutionHandler DISCARD = (executor, task, cause) ->
          LoggerFactory.getLogger(Future.class).warn("Failed to submit a task to {}; the task is discarded",
                  executor.getClass().getName(), cause);

  /**
   * Handle a listener notification task rejected by the given {@link Executor}.
   *
   * @param executor the executor that rejected the submission
   * @param task the rejected listener notification task; the handler decides
   * whether and where to run it
   * @param cause the {@link RejectedExecutionException} reported by the executor
   * @throws RuntimeException any exception thrown by this handler propagates to the
   * caller that completed the future (e.g. {@code setSuccess()}, {@code setFailure()},
   * or {@code cancel()})
   */
  void handleRejected(Executor executor, Runnable task, RejectedExecutionException cause);

}