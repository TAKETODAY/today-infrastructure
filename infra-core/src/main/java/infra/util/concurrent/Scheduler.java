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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import infra.util.InfraStrategies;

/**
 * An {@link Executor} with support for one-shot delayed tasks.
 *
 * <p>{@link #execute(Runnable)} submits a task without an explicit delay, while
 * {@link #schedule(Runnable, long, TimeUnit)} returns a {@link ScheduledFuture}
 * that tracks completion of a delayed task. Implementations determine the
 * execution threads and may use separate executors for these operations.
 *
 * <p>Use {@link #lookup()} to resolve a scheduler through configured strategies,
 * or {@link Future#defaultScheduler} to reuse the shared scheduler used by
 * {@link Future}:
 * <pre>{@code
 * Scheduler scheduler = Future.defaultScheduler;
 * scheduler.execute(() -> System.out.println("Task executed"));
 * scheduler.schedule(() -> System.out.println("Delayed task executed"), 5, TimeUnit.SECONDS);
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Future#timeout
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ScheduledExecutorService
 * @since 5.0 2024/8/5 15:25
 */
public interface Scheduler extends Executor {

  /**
   * Executes the given command at some time in the future. The command
   * may execute in a new thread, in a pooled thread, or in the calling
   * thread, at the discretion of the {@code Executor} implementation.
   *
   * @param command the runnable task
   * @throws RejectedExecutionException if this task cannot be
   * accepted for execution
   * @throws NullPointerException if {@code command} is {@code null}
   */
  @Override
  void execute(Runnable command);

  /**
   * Submits a one-shot task that becomes enabled after the given delay.
   *
   * <p>A zero or negative delay enables immediate execution. Becoming enabled
   * does not guarantee that the task starts immediately: execution depends on
   * the availability of the implementation's execution resources.
   *
   * <p>The returned future tracks the task itself, rather than its submission
   * to another executor. Its {@code get()} method returns {@code null} on
   * successful completion and reports task failures via
   * {@link java.util.concurrent.ExecutionException}.
   *
   * @param command the task to execute
   * @param delay the time from now to delay execution
   * @param unit the time unit of the delay parameter
   * @return a future representing completion of the task
   * @throws RejectedExecutionException if the task cannot be
   * scheduled for execution
   * @throws NullPointerException if {@code command} or {@code unit} is {@code null}
   */
  ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

  /**
   * Resolves a scheduler through {@link InfraStrategies} in the following order:
   * <ol>
   * <li>Create a scheduler using the first available {@link SchedulerFactory}.</li>
   * <li>Load the first available {@code Scheduler} strategy.</li>
   * <li>Create a {@link DefaultScheduler} if neither strategy is available.</li>
   * </ol>
   *
   * <p>This method does not cache its result. In particular, each fallback lookup
   * creates a new default scheduler with its own scheduled thread pool. Use
   * {@link Future#defaultScheduler} when a shared scheduler is required.
   *
   * @return the resolved scheduler
   * @see SchedulerFactory#create()
   * @see Future#defaultScheduler
   */
  static Scheduler lookup() {
    var factory = InfraStrategies.findFirst(SchedulerFactory.class, null);
    if (factory == null) {
      Scheduler scheduler = InfraStrategies.findFirst(Scheduler.class, null);
      if (scheduler == null) {
        return new DefaultScheduler();
      }
      return scheduler;
    }
    return factory.create();
  }

}
