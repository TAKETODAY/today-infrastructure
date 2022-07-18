/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.support.DelegatingErrorHandlingRunnable;
import cn.taketoday.scheduling.support.SimpleTriggerContext;
import cn.taketoday.util.ErrorHandler;

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

  public ReschedulingRunnable(
          Runnable delegate, Trigger trigger, Clock clock,
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
      this.currentFuture = executor.schedule(this, initialDelay.toMillis(), TimeUnit.MILLISECONDS);
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
    long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
    return (diff == 0 ? 0 : ((diff < 0) ? -1 : 1));
  }

}
