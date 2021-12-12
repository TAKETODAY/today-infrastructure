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

package cn.taketoday.context.event;

import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ErrorHandler;
import cn.taketoday.util.ExceptionUtils;

/**
 * Simple implementation of the {@link ApplicationEventMulticaster} interface.
 *
 * <p>Multicasts all events to all registered listeners, leaving it up to
 * the listeners to ignore events that they are not interested in.
 * Listeners will usually perform corresponding {@code instanceof}
 * checks on the passed-in event object.
 *
 * <p>By default, all listeners are invoked in the calling thread.
 * This allows the danger of a rogue listener blocking the entire application,
 * but adds minimal overhead. Specify an alternative task executor to have
 * listeners executed in different threads, for example from a thread pool.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see #setTaskExecutor
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

  @Nullable
  private Executor taskExecutor;

  @Nullable
  private ErrorHandler errorHandler;

  /**
   * Create a new SimpleApplicationEventMulticaster.
   */
  public SimpleApplicationEventMulticaster() { }

  /**
   * Create a new SimpleApplicationEventMulticaster for the given BeanFactory.
   */
  public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
    setBeanFactory(beanFactory);
  }

  /**
   * Set a custom executor (typically a {@link cn.taketoday.core.task.TaskExecutor})
   * to invoke each listener with.
   * <p>Default is equivalent to {@link cn.taketoday.core.task.SyncTaskExecutor},
   * executing all listeners synchronously in the calling thread.
   * <p>Consider specifying an asynchronous task executor here to not block the
   * caller until all listeners have been executed. However, note that asynchronous
   * execution will not participate in the caller's thread context (class loader,
   * transaction association) unless the TaskExecutor explicitly supports this.
   *
   * @see cn.taketoday.core.task.SyncTaskExecutor
   * @see cn.taketoday.core.task.SimpleAsyncTaskExecutor
   */
  public void setTaskExecutor(@Nullable Executor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Return the current task executor for this multicaster.
   */
  @Nullable
  protected Executor getTaskExecutor() {
    return this.taskExecutor;
  }

  /**
   * Set the {@link ErrorHandler} to invoke in case an exception is thrown
   * from a listener.
   * <p>Default is none, with a listener exception stopping the current
   * multicast and getting propagated to the publisher of the current event.
   * If a {@linkplain #setTaskExecutor task executor} is specified, each
   * individual listener exception will get propagated to the executor but
   * won't necessarily stop execution of other listeners.
   * <p>Consider setting an {@link ErrorHandler} implementation that catches
   * and logs exceptions (
   * {@link cn.taketoday.scheduling.support.TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER})
   * or an implementation that logs exceptions while nevertheless propagating them
   * (e.g. {@link cn.taketoday.scheduling.support.TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}).
   */
  public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  /**
   * Return the current error handler for this multicaster.
   */
  @Nullable
  protected ErrorHandler getErrorHandler() {
    return this.errorHandler;
  }

  @Override
  public void multicastEvent(Object event) {
    multicastEvent(event, resolveDefaultEventType(event));
  }

  @Override
  public void multicastEvent(final Object event, @Nullable ResolvableType eventType) {
    if (eventType == null) {
      eventType = resolveDefaultEventType(event);
    }
    Executor executor = getTaskExecutor();
    if (executor != null) {
      for (ApplicationListener<?> listener : getApplicationListeners(event, eventType)) {
        executor.execute(() -> invokeListener(listener, event));
      }
    }
    else {
      for (ApplicationListener<?> listener : getApplicationListeners(event, eventType)) {
        invokeListener(listener, event);
      }
    }
  }

  private ResolvableType resolveDefaultEventType(Object event) {
    return ResolvableType.fromInstance(event);
  }

  /**
   * Invoke the given listener with the given event.
   *
   * @param listener the ApplicationListener to invoke
   * @param event the current event to propagate
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void invokeListener(ApplicationListener listener, Object event) {
    try {
      listener.onApplicationEvent(event);
    }
    catch (Throwable ex) {
      ErrorHandler errorHandler = getErrorHandler();
      if (errorHandler != null) {
        errorHandler.handleError(ex);
      }
      else {
        throw ExceptionUtils.sneakyThrow(ex);
      }
    }
  }

}
