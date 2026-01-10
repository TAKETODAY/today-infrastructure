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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import infra.lang.Assert;
import infra.scheduling.Trigger;
import infra.scheduling.support.DelegatingErrorHandlingRunnable;
import infra.scheduling.support.SimpleTriggerContext;
import infra.util.ErrorHandler;

/**
 * Internal adapter that reschedules an underlying {@link Runnable} according
 * to the next execution time suggested by a given {@link Trigger}.
 *
 * <p>Necessary because a native {@link ScheduledExecutorService} supports
 * delay-driven execution only. The flexibility of the {@link Trigger} interface
 * will be translated onto a delay for the next execution time (repeatedly).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
class ReschedulingRunnable extends DelegatingErrorHandlingRunnable implements ScheduledFuture<Object> {

  private final Trigger trigger;

  private final SimpleTriggerContext triggerContext;

  private final ScheduledExecutorService executor;

  @Nullable
  private ScheduledFuture<?> currentFuture;

  @Nullable
  private Instant scheduledExecutionTime;

  private final Object triggerContextMonitor = new Object();

  public ReschedulingRunnable(Runnable delegate, Trigger trigger, Clock clock,
          ScheduledExecutorService executor, ErrorHandler errorHandler) {

    super(delegate, errorHandler);
    this.trigger = trigger;
    this.triggerContext = new SimpleTriggerContext(clock);
    this.executor = executor;
  }

  @Nullable
  public ScheduledFuture<?> schedule() {
    synchronized(this.triggerContextMonitor) {
      this.scheduledExecutionTime = trigger.nextExecution(this.triggerContext);
      if (this.scheduledExecutionTime == null) {
        return null;
      }
      Duration initialDelay = Duration.between(triggerContext.getClock().instant(), scheduledExecutionTime);
      this.currentFuture = executor.schedule(this, initialDelay.toNanos(), TimeUnit.NANOSECONDS);
      return this;
    }
  }

  private ScheduledFuture<?> obtainCurrentFuture() {
    Assert.state(this.currentFuture != null, "No scheduled future");
    return this.currentFuture;
  }

  @Override
  public void run() {
    Instant actualExecutionTime = triggerContext.getClock().instant();
    super.run();
    Instant completionTime = triggerContext.getClock().instant();
    synchronized(this.triggerContextMonitor) {
      Assert.state(this.scheduledExecutionTime != null, "No scheduled execution");
      this.triggerContext.update(this.scheduledExecutionTime, actualExecutionTime, completionTime);
      if (!obtainCurrentFuture().isCancelled()) {
        schedule();
      }
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    synchronized(this.triggerContextMonitor) {
      return obtainCurrentFuture().cancel(mayInterruptIfRunning);
    }
  }

  @Override
  public boolean isCancelled() {
    synchronized(this.triggerContextMonitor) {
      return obtainCurrentFuture().isCancelled();
    }
  }

  @Override
  public boolean isDone() {
    synchronized(this.triggerContextMonitor) {
      return obtainCurrentFuture().isDone();
    }
  }

  @Override
  public Object get() throws InterruptedException, ExecutionException {
    ScheduledFuture<?> curr;
    synchronized(this.triggerContextMonitor) {
      curr = obtainCurrentFuture();
    }
    return curr.get();
  }

  @Override
  public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    ScheduledFuture<?> curr;
    synchronized(this.triggerContextMonitor) {
      curr = obtainCurrentFuture();
    }
    return curr.get(timeout, unit);
  }

  @Override
  public long getDelay(TimeUnit unit) {
    ScheduledFuture<?> curr;
    synchronized(this.triggerContextMonitor) {
      curr = obtainCurrentFuture();
    }
    return curr.getDelay(unit);
  }

  @Override
  public int compareTo(Delayed other) {
    if (this == other) {
      return 0;
    }
    long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
    return (diff == 0 ? 0 : (diff < 0 ? -1 : 1));
  }

}
