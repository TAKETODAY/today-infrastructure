/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling.concurrent;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import infra.core.task.AsyncTaskExecutor;
import infra.core.task.TaskDecorator;
import infra.core.task.TaskExecutor;
import infra.core.task.support.TaskExecutorAdapter;
import infra.scheduling.SchedulingAwareRunnable;
import infra.scheduling.SchedulingTaskExecutor;
import infra.util.ClassUtils;
import infra.util.concurrent.Future;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedExecutors;
import jakarta.enterprise.concurrent.ManagedTask;

/**
 * Adapter that takes a {@code java.util.concurrent.Executor} and exposes
 * a Framework {@link TaskExecutor} for it.
 * Also detects an extended {@code java.util.concurrent.ExecutorService}, adapting
 * the {@link AsyncTaskExecutor} interface accordingly.
 *
 * <p>Autodetects a JSR-236 {@link ManagedExecutorService}
 * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it,
 * exposing a long-running hint based on {@link SchedulingAwareRunnable} and an identity
 * name based on the given Runnable/Callable's {@code toString()}. For JSR-236 style
 * lookup in a Jakarta EE environment, consider using {@link DefaultManagedTaskExecutor}.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskExecutor} that allows
 * for defining a {@link java.util.concurrent.ThreadPoolExecutor} in bean style,
 * exposing it as a Framework {@link TaskExecutor} directly.
 * This is a convenient alternative to a raw ThreadPoolExecutor definition with
 * a separate definition of the present adapter class.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see Executors
 * @see DefaultManagedTaskExecutor
 * @see ThreadPoolTaskExecutor
 * @since 4.0
 */
public class ConcurrentTaskExecutor implements AsyncTaskExecutor, SchedulingTaskExecutor {

  private static final Executor STUB_EXECUTOR = task -> {
    throw new IllegalStateException("Executor not configured");
  };

  @Nullable
  private static final Class<?> managedExecutorServiceClass = ClassUtils.load(
          "jakarta.enterprise.concurrent.ManagedExecutorService", ConcurrentTaskScheduler.class.getClassLoader()
  );

  private Executor concurrentExecutor = STUB_EXECUTOR;

  private TaskExecutorAdapter adaptedExecutor = new TaskExecutorAdapter(STUB_EXECUTOR);

  @Nullable
  private TaskDecorator taskDecorator;

  /**
   * Create a new ConcurrentTaskExecutor, using a single thread executor as default.
   *
   * @see java.util.concurrent.Executors#newSingleThreadExecutor()
   */
  public ConcurrentTaskExecutor() {
    this.concurrentExecutor = Executors.newSingleThreadExecutor();
    this.adaptedExecutor = new TaskExecutorAdapter(this.concurrentExecutor);
  }

  /**
   * Create a new ConcurrentTaskExecutor, using the given {@link Executor}.
   * <p>Autodetects a JSR-236 {@link ManagedExecutorService}
   * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it.
   *
   * @param executor the {@link Executor} to delegate to
   */
  public ConcurrentTaskExecutor(@Nullable Executor executor) {
    if (executor != null) {
      setConcurrentExecutor(executor);
    }
  }

  /**
   * Specify the {@link Executor} to delegate to.
   * <p>Autodetects a JSR-236 {@link ManagedExecutorService}
   * in order to expose {@link jakarta.enterprise.concurrent.ManagedTask} adapters for it.
   */
  public final void setConcurrentExecutor(Executor executor) {
    this.concurrentExecutor = executor;
    this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
  }

  /**
   * Return the {@link Executor} that this adapter delegates to.
   */
  public final Executor getConcurrentExecutor() {
    return this.concurrentExecutor;
  }

  /**
   * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
   * about to be executed.
   * <p>Note that such a decorator is not necessarily being applied to the
   * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
   * execution callback (which may be a wrapper around the user-supplied task).
   * <p>The primary use case is to set some execution context around the task's
   * invocation, or to provide some monitoring/statistics for task execution.
   */
  public final void setTaskDecorator(TaskDecorator taskDecorator) {
    this.taskDecorator = taskDecorator;
    this.adaptedExecutor.setTaskDecorator(taskDecorator);
  }

  @Override
  public void execute(Runnable task) {
    this.adaptedExecutor.execute(task);
  }

  @Override
  public void execute(Runnable task, long startTimeout) {
    this.adaptedExecutor.execute(task, startTimeout);
  }

  @Override
  public Future<Void> submit(Runnable task) {
    return this.adaptedExecutor.submit(task);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return this.adaptedExecutor.submit(task);
  }

  private TaskExecutorAdapter getAdaptedExecutor(Executor originalExecutor) {
    var adapter = managedExecutorServiceClass != null && managedExecutorServiceClass.isInstance(originalExecutor)
            ? new ManagedTaskExecutorAdapter(originalExecutor)
            : new TaskExecutorAdapter(originalExecutor);
    if (this.taskDecorator != null) {
      adapter.setTaskDecorator(this.taskDecorator);
    }
    return adapter;
  }

  Runnable decorateTaskIfNecessary(Runnable task) {
    return this.taskDecorator != null ? this.taskDecorator.decorate(task) : task;
  }

  /**
   * TaskExecutorAdapter subclass that wraps all provided Runnables and Callables
   * with a JSR-236 ManagedTask, exposing a long-running hint based on
   * {@link SchedulingAwareRunnable} and an identity name based on the task's
   * {@code toString()} representation.
   */
  private static class ManagedTaskExecutorAdapter extends TaskExecutorAdapter {

    public ManagedTaskExecutorAdapter(Executor concurrentExecutor) {
      super(concurrentExecutor);
    }

    @Override
    public void execute(Runnable task) {
      super.execute(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
    }

    @Override
    public Future<Void> submit(Runnable task) {
      return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
    }

  }

  /**
   * Delegate that wraps a given Runnable/Callable  with a JSR-236 ManagedTask,
   * exposing a long-running hint based on {@link SchedulingAwareRunnable}
   * and a given identity name.
   */
  protected static class ManagedTaskBuilder {

    public static Runnable buildManagedTask(Runnable task, String identityName) {
      Map<String, String> properties;
      if (task instanceof SchedulingAwareRunnable sar) {
        properties = new HashMap<>(4);
        properties.put(ManagedTask.LONGRUNNING_HINT, Boolean.toString(sar.isLongLived()));
      }
      else {
        properties = new HashMap<>(2);
      }
      properties.put(ManagedTask.IDENTITY_NAME, identityName);
      return ManagedExecutors.managedTask(task, properties, null);
    }

    public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
      Map<String, String> properties = new HashMap<>(2);
      properties.put(ManagedTask.IDENTITY_NAME, identityName);
      return ManagedExecutors.managedTask(task, properties, null);
    }
  }

}
