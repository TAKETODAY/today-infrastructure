/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.event;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import infra.beans.factory.BeanFactory;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.context.PayloadApplicationEvent;
import infra.core.ResolvableType;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.core.task.SyncTaskExecutor;
import infra.core.task.TaskExecutor;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.scheduling.support.TaskUtils;
import infra.util.ErrorHandler;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setTaskExecutor
 * @since 4.0
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

  /**
   * The current task executor for this multicaster.
   */
  @Nullable
  protected Executor taskExecutor;

  /**
   * The current error handler for this multicaster.
   */
  @Nullable
  protected ErrorHandler errorHandler;

  @Nullable
  private volatile Logger lazyLogger;

  /**
   * Create a new SimpleApplicationEventMulticaster.
   */
  public SimpleApplicationEventMulticaster() {

  }

  /**
   * Create a new SimpleApplicationEventMulticaster for the given BeanFactory.
   */
  public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
    setBeanFactory(beanFactory);
  }

  /**
   * Set a custom executor (typically a {@link TaskExecutor})
   * to invoke each listener with.
   * <p>Default is equivalent to {@link SyncTaskExecutor},
   * executing all listeners synchronously in the calling thread.
   * <p>Consider specifying an asynchronous task executor here to not block the caller
   * until all listeners have been executed. However, note that asynchronous execution
   * will not participate in the caller's thread context (class loader, transaction context)
   * unless the TaskExecutor explicitly supports this.
   * <p>{@link ApplicationListener} instances which declare no support for asynchronous
   * execution ({@link ApplicationListener#supportsAsyncExecution()} always run within
   * the original thread which published the event, e.g. the transaction-synchronized
   * {@link infra.transaction.event.TransactionalApplicationListener}.
   *
   * @see SyncTaskExecutor
   * @see SimpleAsyncTaskExecutor
   */
  public void setTaskExecutor(@Nullable Executor taskExecutor) {
    this.taskExecutor = taskExecutor;
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
   * {@link TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER})
   * or an implementation that logs exceptions while nevertheless propagating them
   * (e.g. {@link TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}).
   */
  public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Override
  public void multicastEvent(ApplicationEvent event) {
    multicastEvent(event, null);
  }

  @Override
  public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
    if (eventType == null) {
      eventType = ResolvableType.forInstance(event);
    }
    Executor executor = taskExecutor;
    if (executor != null) {
      for (ApplicationListener<?> listener : getApplicationListeners(event, eventType)) {
        if (listener.supportsAsyncExecution()) {
          try {
            executor.execute(() -> invokeListener(listener, event));
          }
          catch (RejectedExecutionException ex) {
            // Probably on shutdown -> invoke listener locally instead
            invokeListener(listener, event);
          }
        }
        else {
          invokeListener(listener, event);
        }
      }
    }
    else {
      for (ApplicationListener<?> listener : getApplicationListeners(event, eventType)) {
        invokeListener(listener, event);
      }
    }
  }

  /**
   * Invoke the given listener with the given event.
   *
   * @param listener the ApplicationListener to invoke
   * @param event the current event to propagate
   */
  protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
    ErrorHandler errorHandler = this.errorHandler;
    if (errorHandler != null) {
      try {
        doInvokeListener(listener, event);
      }
      catch (Throwable err) {
        errorHandler.handleError(err);
      }
    }
    else {
      doInvokeListener(listener, event);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
    try {
      listener.onApplicationEvent(event);
    }
    catch (ClassCastException ex) {
      String msg = ex.getMessage();
      if (msg == null || matchesClassCastMessage(msg, event.getClass())
              || (
              event instanceof PayloadApplicationEvent pae
                      && matchesClassCastMessage(msg, pae.getPayload().getClass()))
      ) {
        // Possibly a lambda-defined listener which we could not resolve the generic event type for
        // -> let's suppress the exception.
        Logger loggerToUse = this.lazyLogger;
        if (loggerToUse == null) {
          loggerToUse = LoggerFactory.getLogger(getClass());
          this.lazyLogger = loggerToUse;
        }
        if (loggerToUse.isTraceEnabled()) {
          loggerToUse.trace("Non-matching event type for listener: {}", listener, ex);
        }
      }
      else {
        throw ex;
      }
    }
  }

  private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {
    // On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
    if (classCastMessage.startsWith(eventClass.getName())) {
      return true;
    }
    // On Java 11, the message starts with "class ..." a.k.a. Class.toString()
    if (classCastMessage.startsWith(eventClass.toString())) {
      return true;
    }
    // On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
    int moduleSeparatorIndex = classCastMessage.indexOf('/');
    // false Assuming an unrelated class cast failure...
    return moduleSeparatorIndex != -1
            && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1);
  }
}
