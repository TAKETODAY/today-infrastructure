/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for setting up a {@link java.util.concurrent.ExecutorService}
 * (typically a {@link java.util.concurrent.ThreadPoolExecutor} or
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}).
 *
 * <p>Defines common configuration settings and common lifecycle handling,
 * inheriting thread customization options (name, priority, etc) from
 * {@link cn.taketoday.util.CustomizableThreadCreator}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
        implements BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean,
        SmartLifecycle, ApplicationListener<ContextClosedEvent> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private ThreadFactory threadFactory = this;

  private boolean threadNamePrefixSet = false;

  private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

  private boolean acceptTasksAfterContextClose = false;

  private boolean waitForTasksToCompleteOnShutdown = false;

  private long awaitTerminationMillis = 0;

  private int phase = DEFAULT_PHASE;

  @Nullable
  private String beanName;

  @Nullable
  private ApplicationContext applicationContext;

  @Nullable
  private ExecutorService executor;

  @Nullable
  private ExecutorLifecycleDelegate lifecycleDelegate;

  private volatile boolean lateShutdown;

  /**
   * Set the ThreadFactory to use for the ExecutorService's thread pool.
   * Default is the underlying ExecutorService's default thread factory.
   * <p>In a Jakarta EE or other managed environment with JSR-236 support,
   * consider specifying a JNDI-located ManagedThreadFactory: by default,
   * to be found at "java:comp/DefaultManagedThreadFactory".
   * Use the "jee:jndi-lookup" namespace element in XML or the programmatic
   * {@link cn.taketoday.jndi.JndiLocatorDelegate} for convenient lookup.
   * Alternatively, consider using {@link DefaultManagedAwareThreadFactory}
   * with its fallback to local threads in case of no managed thread factory found.
   *
   * @see java.util.concurrent.Executors#defaultThreadFactory()
   * @see jakarta.enterprise.concurrent.ManagedThreadFactory
   * @see DefaultManagedAwareThreadFactory
   */
  public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
    this.threadFactory = (threadFactory != null ? threadFactory : this);
  }

  @Override
  public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
    super.setThreadNamePrefix(threadNamePrefix);
    this.threadNamePrefixSet = true;
  }

  /**
   * Set the RejectedExecutionHandler to use for the ExecutorService.
   * Default is the ExecutorService's default abort policy.
   *
   * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
   */
  public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
    this.rejectedExecutionHandler =
            (rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
  }

  /**
   * Set whether to accept further tasks after the application context close phase
   * has begun.
   * <p>Default is {@code false}, triggering an early soft shutdown of
   * the executor and therefore rejecting any further task submissions. Switch this
   * to {@code true} in order to let other components submit tasks even during their
   * own destruction callbacks, at the expense of a longer shutdown phase.
   * This will usually go along with
   * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}.
   * <p>This flag will only have effect when the executor is running in a Infra
   * application context and able to receive the {@link ContextClosedEvent}.
   *
   * @see cn.taketoday.context.ConfigurableApplicationContext#close()
   * @see DisposableBean#destroy()
   * @see #shutdown()
   */
  public void setAcceptTasksAfterContextClose(boolean acceptTasksAfterContextClose) {
    this.acceptTasksAfterContextClose = acceptTasksAfterContextClose;
  }

  /**
   * Set whether to wait for scheduled tasks to complete on shutdown,
   * not interrupting running tasks and executing all tasks in the queue.
   * <p>Default is {@code false}, shutting down immediately through interrupting
   * ongoing tasks and clearing the queue. Switch this flag to {@code true} if
   * you prefer fully completed tasks at the expense of a longer shutdown phase.
   * <p>Note that Infra container shutdown continues while ongoing tasks
   * are being completed. If you want this executor to block and wait for the
   * termination of tasks before the rest of the container continues to shut
   * down - e.g. in order to keep up other resources that your tasks may need -,
   * set the {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}
   * property instead of or in addition to this property.
   *
   * @see java.util.concurrent.ExecutorService#shutdown()
   * @see java.util.concurrent.ExecutorService#shutdownNow()
   */
  public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
    this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
  }

  /**
   * Set the maximum number of seconds that this executor is supposed to block
   * on shutdown in order to wait for remaining tasks to complete their execution
   * before the rest of the container continues to shut down. This is particularly
   * useful if your remaining tasks are likely to need access to other resources
   * that are also managed by the container.
   * <p>By default, this executor won't wait for the termination of tasks at all.
   * It will either shut down immediately, interrupting ongoing tasks and clearing
   * the remaining task queue - or, if the
   * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
   * flag has been set to {@code true}, it will continue to fully execute all
   * ongoing tasks as well as all remaining tasks in the queue, in parallel to
   * the rest of the container shutting down.
   * <p>In either case, if you specify an await-termination period using this property,
   * this executor will wait for the given time (max) for the termination of tasks.
   * As a rule of thumb, specify a significantly higher timeout here if you set
   * "waitForTasksToCompleteOnShutdown" to {@code true} at the same time,
   * since all remaining tasks in the queue will still get executed - in contrast
   * to the default shutdown behavior where it's just about waiting for currently
   * executing tasks that aren't reacting to thread interruption.
   *
   * @see #setAwaitTerminationMillis
   * @see java.util.concurrent.ExecutorService#shutdown()
   * @see java.util.concurrent.ExecutorService#awaitTermination
   */
  public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
    this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
  }

  /**
   * Variant of {@link #setAwaitTerminationSeconds} with millisecond precision.
   *
   * @see #setAwaitTerminationSeconds
   */
  public void setAwaitTerminationMillis(long awaitTerminationMillis) {
    this.awaitTerminationMillis = awaitTerminationMillis;
  }

  /**
   * Specify the lifecycle phase for pausing and resuming this executor.
   * The default is {@link #DEFAULT_PHASE}.
   *
   * @see SmartLifecycle#getPhase()
   */
  public void setPhase(int phase) {
    this.phase = phase;
  }

  /**
   * Return the lifecycle phase for pausing and resuming this executor.
   *
   * @see #setPhase
   */
  @Override
  public int getPhase() {
    return this.phase;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Calls {@code initialize()} after the container applied all property values.
   *
   * @see #initialize()
   */
  @Override
  public void afterPropertiesSet() {
    initialize();
  }

  /**
   * Set up the ExecutorService.
   */
  public void initialize() {
    if (logger.isDebugEnabled()) {
      logger.debug("Initializing ExecutorService{}",
              (this.beanName != null ? " '" + this.beanName + "'" : ""));
    }
    if (!this.threadNamePrefixSet && this.beanName != null) {
      setThreadNamePrefix(this.beanName + "-");
    }
    this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
    this.lifecycleDelegate = new ExecutorLifecycleDelegate(this.executor);
  }

  /**
   * Create the target {@link java.util.concurrent.ExecutorService} instance.
   * Called by {@code afterPropertiesSet}.
   *
   * @param threadFactory the ThreadFactory to use
   * @param rejectedExecutionHandler the RejectedExecutionHandler to use
   * @return a new ExecutorService instance
   * @see #afterPropertiesSet()
   */
  protected abstract ExecutorService initializeExecutor(
          ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);

  /**
   * Calls {@code shutdown} when the BeanFactory destroys the executor instance.
   *
   * @see #shutdown()
   */
  @Override
  public void destroy() {
    shutdown();
  }

  /**
   * Initiate a shutdown on the underlying ExecutorService,
   * rejecting further task submissions.
   * <p>The executor will not accept further tasks and will prevent further
   * scheduling of periodic tasks, letting existing tasks complete still.
   * This step is non-blocking and can be applied as an early shutdown signal
   * before following up with a full {@link #shutdown()} call later on.
   *
   * @see #shutdown()
   * @see java.util.concurrent.ExecutorService#shutdown()
   */
  public void initiateShutdown() {
    if (this.executor != null) {
      this.executor.shutdown();
    }
  }

  /**
   * Perform a full shutdown on the underlying ExecutorService,
   * according to the corresponding configuration settings.
   * <p>This step potentially blocks for the configured termination period,
   * waiting for remaining tasks to complete. For an early shutdown signal
   * to not accept further tasks, call {@link #initiateShutdown()} first.
   *
   * @see #setWaitForTasksToCompleteOnShutdown
   * @see #setAwaitTerminationMillis
   * @see java.util.concurrent.ExecutorService#shutdown()
   * @see java.util.concurrent.ExecutorService#shutdownNow()
   * @see java.util.concurrent.ExecutorService#awaitTermination
   */
  public void shutdown() {
    if (logger.isDebugEnabled()) {
      logger.debug("Shutting down ExecutorService{}", (this.beanName != null ? " '" + this.beanName + "'" : ""));
    }
    if (this.executor != null) {
      if (this.waitForTasksToCompleteOnShutdown) {
        this.executor.shutdown();
      }
      else {
        for (Runnable remainingTask : this.executor.shutdownNow()) {
          cancelRemainingTask(remainingTask);
        }
      }
      awaitTerminationIfNecessary(this.executor);
    }
  }

  /**
   * Cancel the given remaining task which never commended execution,
   * as returned from {@link ExecutorService#shutdownNow()}.
   *
   * @param task the task to cancel (typically a {@link RunnableFuture})
   * @see #shutdown()
   * @see RunnableFuture#cancel(boolean)
   */
  protected void cancelRemainingTask(Runnable task) {
    if (task instanceof Future<?> future) {
      future.cancel(true);
    }
  }

  /**
   * Wait for the executor to terminate, according to the value of the
   * {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} property.
   */
  private void awaitTerminationIfNecessary(ExecutorService executor) {
    if (this.awaitTerminationMillis > 0) {
      try {
        if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
          if (logger.isWarnEnabled()) {
            logger.warn("Timed out while waiting for executor" +
                    (this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
          }
        }
      }
      catch (InterruptedException ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("Interrupted while waiting for executor" +
                  (this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
        }
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Resume this executor if paused before (otherwise a no-op).
   */
  @Override
  public void start() {
    if (lifecycleDelegate != null) {
      lifecycleDelegate.start();
    }
  }

  /**
   * Pause this executor, not waiting for tasks to complete.
   */
  @Override
  public void stop() {
    if (this.lifecycleDelegate != null && !this.lateShutdown) {
      this.lifecycleDelegate.stop();
    }
  }

  /**
   * Pause this executor, triggering the given callback
   * once all currently executing tasks have completed.
   */
  @Override
  public void stop(Runnable callback) {
    if (this.lifecycleDelegate != null && !this.lateShutdown) {
      this.lifecycleDelegate.stop(callback);
    }
    else {
      callback.run();
    }
  }

  /**
   * Check whether this executor is not paused and has not been shut down either.
   *
   * @see #start()
   * @see #stop()
   */
  @Override
  public boolean isRunning() {
    return lifecycleDelegate != null && lifecycleDelegate.isRunning();
  }

  /**
   * A before-execute callback for framework subclasses to delegate to
   * (for start/stop handling), and possibly also for custom subclasses
   * to extend (making sure to call this implementation as well).
   *
   * @param thread the thread to run the task
   * @param task the task to be executed
   * @see ThreadPoolExecutor#beforeExecute(Thread, Runnable)
   */
  protected void beforeExecute(Thread thread, Runnable task) {
    if (lifecycleDelegate != null) {
      lifecycleDelegate.beforeExecute(thread);
    }
  }

  /**
   * An after-execute callback for framework subclasses to delegate to
   * (for start/stop handling), and possibly also for custom subclasses
   * to extend (making sure to call this implementation as well).
   *
   * @param task the task that has been executed
   * @param ex the exception thrown during execution, if any
   * @see ThreadPoolExecutor#afterExecute(Runnable, Throwable)
   */
  protected void afterExecute(Runnable task, @Nullable Throwable ex) {
    if (lifecycleDelegate != null) {
      lifecycleDelegate.afterExecute();
    }
  }

  /**
   * {@link ContextClosedEvent} handler for initiating an early shutdown.
   *
   * @see #initiateShutdown()
   */
  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    if (event.getApplicationContext() == this.applicationContext) {
      if (this.acceptTasksAfterContextClose || this.waitForTasksToCompleteOnShutdown) {
        // Late shutdown without early stop lifecycle.
        this.lateShutdown = true;
      }
      else {
        // Early shutdown signal: accept no further tasks, let existing tasks complete
        // before hitting the actual destruction step in the shutdown() method above.
        initiateShutdown();
      }
    }
  }

}
