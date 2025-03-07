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

package infra.web.async;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import infra.web.RequestContext;

/**
 * {@code DeferredResult} provides an alternative to using a {@link Callable} for
 * asynchronous request processing. While a {@code Callable} is executed concurrently
 * on behalf of the application, with a {@code DeferredResult} the application can
 * produce the result from a thread of its choice.
 *
 * <p>Subclasses can extend this class to easily associate additional data or behavior
 * with the {@link DeferredResult}. For example, one might want to associate the user
 * used to create the {@link DeferredResult} by extending the class and adding an
 * additional property for the user. In this way, the user could easily be accessed
 * later without the need to use a data structure to do the mapping.
 *
 * <p>An example of associating additional behavior to this class might be realized
 * by extending the class to implement an additional interface. For example, one
 * might want to implement {@link Comparable} so that when the {@link DeferredResult}
 * is added to a {@link PriorityQueue} it is handled in the correct order.
 *
 * @param <T> the result type
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Rob Winch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DeferredResult<T> implements FutureListener<Future<T>> {

  private static final Object RESULT_NONE = new Object();

  private static final Logger logger = LoggerFactory.getLogger(DeferredResult.class);

  @Nullable
  private final Long timeoutValue;

  private final Supplier<?> timeoutResult;

  @Nullable
  private Runnable timeoutCallback;

  @Nullable
  private Consumer<Throwable> errorCallback;

  @Nullable
  private Runnable completionCallback;

  @Nullable
  private DeferredResultHandler resultHandler;

  @Nullable
  private volatile Object result = RESULT_NONE;

  private volatile boolean expired;

  /**
   * Create a DeferredResult.
   */
  public DeferredResult() {
    this(null);
  }

  /**
   * Create a DeferredResult with a custom timeout value.
   * <p>By default not set in which case the default configured in the MVC
   * Java Config or the MVC namespace is used, or if that's not set, then the
   * timeout depends on the default of the underlying server.
   *
   * @param timeoutValue timeout value in milliseconds
   */
  public DeferredResult(@Nullable Long timeoutValue) {
    this(timeoutValue, () -> RESULT_NONE);
  }

  /**
   * Create a DeferredResult with a timeout value and a default result to use
   * in case of timeout.
   *
   * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
   * @param timeoutResult the result to use
   */
  public DeferredResult(@Nullable Long timeoutValue, Object timeoutResult) {
    this(timeoutValue, () -> timeoutResult);
  }

  /**
   * Variant of {@link #DeferredResult(Long, Object)} that accepts a dynamic
   * fallback value based on a {@link Supplier}.
   *
   * @param timeoutValue timeout value in milliseconds (ignored if {@code null})
   * @param timeoutResult the result supplier to use
   */
  public DeferredResult(@Nullable Long timeoutValue, Supplier<?> timeoutResult) {
    this.timeoutValue = timeoutValue;
    this.timeoutResult = timeoutResult;
  }

  /**
   * Return {@code true} if this DeferredResult is no longer usable either
   * because it was previously set or because the underlying request expired.
   * <p>The result may have been set with a call to {@link #setResult(Object)},
   * or {@link #setErrorResult(Object)}, or as a result of a timeout, if a
   * timeout result was provided to the constructor. The request may also
   * expire due to a timeout or network error.
   */
  public final boolean isSetOrExpired() {
    return result != RESULT_NONE || this.expired;
  }

  /**
   * Return {@code true} if the DeferredResult has been set.
   */
  public boolean hasResult() {
    return result != RESULT_NONE;
  }

  /**
   * Return the result, or {@code null} if the result wasn't set. Since the result
   * can also be {@code null}, it is recommended to use {@link #hasResult()} first
   * to check if there is a result prior to calling this method.
   */
  @Nullable
  public Object getResult() {
    Object resultToCheck = this.result;
    return (resultToCheck != RESULT_NONE ? resultToCheck : null);
  }

  /**
   * Return the configured timeout value in milliseconds.
   */
  @Nullable
  final Long getTimeoutValue() {
    return this.timeoutValue;
  }

  /**
   * Register code to invoke when the async request times out.
   * <p>This method is called from a container thread when an async request
   * times out before the {@code DeferredResult} has been populated.
   * It may invoke {@link DeferredResult#setResult setResult} or
   * {@link DeferredResult#setErrorResult setErrorResult} to resume processing.
   */
  public void onTimeout(Runnable callback) {
    this.timeoutCallback = callback;
  }

  /**
   * Register code to invoke when an error occurred during the async request.
   * <p>This method is called from a container thread when an error occurs
   * while processing an async request before the {@code DeferredResult} has
   * been populated. It may invoke {@link DeferredResult#setResult setResult}
   * or {@link DeferredResult#setErrorResult setErrorResult} to resume
   * processing.
   */
  public void onError(Consumer<Throwable> callback) {
    this.errorCallback = callback;
  }

  /**
   * Register code to invoke when the async request completes.
   * <p>This method is called from a container thread when an async request
   * completed for any reason including timeout and network error. This is useful
   * for detecting that a {@code DeferredResult} instance is no longer usable.
   */
  public void onCompletion(@Nullable Runnable callback) {
    this.completionCallback = callback;
  }

  /**
   * Provide a handler to use to handle the result value.
   *
   * @param resultHandler the handler
   * @see DeferredResultProcessingInterceptor
   */
  public final void setResultHandler(DeferredResultHandler resultHandler) {
    Assert.notNull(resultHandler, "DeferredResultHandler is required");
    // Immediate expiration check outside of the result lock
    if (this.expired) {
      return;
    }
    Object resultToHandle;
    synchronized(this) {
      // Got the lock in the meantime: double-check expiration status
      if (this.expired) {
        return;
      }
      resultToHandle = this.result;
      if (resultToHandle == RESULT_NONE) {
        // No result yet: store handler for processing once it comes in
        this.resultHandler = resultHandler;
        return;
      }
    }
    // If we get here, we need to process an existing result object immediately.
    // The decision is made within the result lock; just the handle call outside
    // of it, avoiding any deadlock potential with container locks.
    try {
      resultHandler.handleResult(resultToHandle);
    }
    catch (Throwable ex) {
      logger.debug("Failed to process async result", ex);
    }
  }

  /**
   * Set the value for the DeferredResult and handle it.
   *
   * @param result the value to set
   * @return {@code true} if the result was set and passed on for handling;
   * {@code false} if the result was already set or the async request expired
   * @see #isSetOrExpired()
   */
  public boolean setResult(@Nullable T result) {
    return setResultInternal(result);
  }

  private boolean setResultInternal(@Nullable Object result) {
    // Immediate expiration check outside of the result lock
    if (isSetOrExpired()) {
      return false;
    }
    DeferredResultHandler resultHandlerToUse;
    synchronized(this) {
      // Got the lock in the meantime: double-check expiration status
      if (isSetOrExpired()) {
        return false;
      }
      // At this point, we got a new result to process
      this.result = result;
      resultHandlerToUse = this.resultHandler;
      if (resultHandlerToUse == null) {
        // No result handler set yet -> let the setResultHandler implementation
        // pick up the result object and invoke the result handler for it.
        return true;
      }
      // Result handler available -> let's clear the stored reference since
      // we don't need it anymore.
      this.resultHandler = null;
    }
    // If we get here, we need to process an existing result object immediately.
    // The decision is made within the result lock; just the handle call outside
    // of it, avoiding any deadlock potential with Web container locks.
    resultHandlerToUse.handleResult(result);
    return true;
  }

  /**
   * Set an error value for the {@link DeferredResult} and handle it.
   * The value may be an {@link Exception} or {@link Throwable} in which case
   * it will be processed as if a handler raised the exception.
   *
   * @param result the error result value
   * @return {@code true} if the result was set to the error value and passed on
   * for handling; {@code false} if the result was already set or the async
   * request expired
   * @see #isSetOrExpired()
   */
  public boolean setErrorResult(@Nullable Object result) {
    return setResultInternal(result);
  }

  // FutureListener

  @Override
  public void operationComplete(Future<T> future) {
    if (future.isSuccess()) {
      setResult(future.getNow());
    }
    else if (!future.isCancelled()) {
      setErrorResult(future.getCause());
    }
  }

  final DeferredResultProcessingInterceptor createInterceptor() {
    return new DeferredResultProcessingInterceptor() {
      @Override
      public <S> boolean handleTimeout(RequestContext request, DeferredResult<S> deferredResult) {
        boolean continueProcessing = true;
        try {
          if (timeoutCallback != null) {
            timeoutCallback.run();
          }
        }
        finally {
          Object value = timeoutResult.get();
          if (value != RESULT_NONE) {
            continueProcessing = false;
            try {
              setResultInternal(value);
            }
            catch (Throwable ex) {
              logger.debug("Failed to handle timeout result", ex);
            }
          }
        }
        return continueProcessing;
      }

      @Override
      public <S> boolean handleError(RequestContext request, DeferredResult<S> deferredResult, Throwable t) {
        try {
          if (errorCallback != null) {
            errorCallback.accept(t);
          }
        }
        finally {
          try {
            setResultInternal(t);
          }
          catch (Throwable ex) {
            logger.debug("Failed to handle error result", ex);
          }
        }
        return false;
      }

      @Override
      public <S> void afterCompletion(RequestContext request, DeferredResult<S> deferredResult) {
        expired = true;
        if (completionCallback != null) {
          completionCallback.run();
        }
      }
    };
  }

}
