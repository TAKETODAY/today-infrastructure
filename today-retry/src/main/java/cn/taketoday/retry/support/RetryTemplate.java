/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.retry.support;

import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.ExhaustedRetryException;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryException;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.TerminatedRetryException;
import cn.taketoday.retry.backoff.BackOffContext;
import cn.taketoday.retry.backoff.BackOffInterruptedException;
import cn.taketoday.retry.backoff.BackOffPolicy;
import cn.taketoday.retry.backoff.NoBackOffPolicy;
import cn.taketoday.retry.policy.MapRetryContextCache;
import cn.taketoday.retry.policy.RetryContextCache;
import cn.taketoday.retry.policy.SimpleRetryPolicy;

/**
 * Template class that simplifies the execution of operations with retry semantics.
 * <p>
 * Retryable operations are encapsulated in implementations of the {@link RetryCallback}
 * interface and are executed using one of the supplied execute methods.
 * <p>
 * By default, an operation is retried if is throws any {@link Exception} or subclass of
 * {@link Exception}. This behaviour can be changed by using the
 * {@link #setRetryPolicy(RetryPolicy)} method.
 * <p>
 * Also by default, each operation is retried for a maximum of three attempts with no back
 * off in between. This behaviour can be configured using the
 * {@link #setRetryPolicy(RetryPolicy)} and {@link #setBackOffPolicy(BackOffPolicy)}
 * properties. The {@link BackOffPolicy} controls how
 * long the pause is between each individual retry attempt.
 * <p>
 * A new instance can be fluently configured via {@link #builder}, e.g: <pre> {@code
 * RetryTemplate.builder()
 *                 .maxAttempts(10)
 *                 .fixedBackoff(1000)
 *                 .build();
 * }</pre> See {@link RetryTemplateBuilder} for more examples and details.
 * <p>
 * This class is thread-safe and suitable for concurrent access when executing operations
 * and when performing configuration changes. As such, it is possible to change the number
 * of retries on the fly, as well as the {@link BackOffPolicy} used and no in progress
 * retryable operations will be affected.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @author Josh Long
 * @author Aleksandr Shamukov
 * @since 4.0
 */
public class RetryTemplate implements RetryOperations {

  /**
   * Retry context attribute name that indicates the context should be considered global
   * state (never closed). TODO: convert this to a flag in the RetryState.
   */
  private static final String GLOBAL_STATE = "state.global";

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private volatile BackOffPolicy backOffPolicy = new NoBackOffPolicy();

  private volatile RetryPolicy retryPolicy = new SimpleRetryPolicy(3);

  private volatile RetryListener[] listeners = new RetryListener[0];

  private RetryContextCache retryContextCache = new MapRetryContextCache();

  private boolean throwLastExceptionOnExhausted;

  /**
   * Main entry point to configure RetryTemplate using fluent API. See
   * {@link RetryTemplateBuilder} for usage examples and details.
   *
   * @return a new instance of RetryTemplateBuilder with preset default behaviour, that
   * can be overwritten during manual configuration
   */
  public static RetryTemplateBuilder builder() {
    return new RetryTemplateBuilder();
  }

  /**
   * Creates a new default instance. The properties of default instance are described in
   * {@link RetryTemplateBuilder} documentation.
   *
   * @return a new instance of RetryTemplate with default behaviour
   */
  public static RetryTemplate defaultInstance() {
    return new RetryTemplateBuilder().build();
  }

  /**
   * @param throwLastExceptionOnExhausted the throwLastExceptionOnExhausted to set
   */
  public void setThrowLastExceptionOnExhausted(boolean throwLastExceptionOnExhausted) {
    this.throwLastExceptionOnExhausted = throwLastExceptionOnExhausted;
  }

  /**
   * Public setter for the {@link RetryContextCache}.
   *
   * @param retryContextCache the {@link RetryContextCache} to set.
   */
  public void setRetryContextCache(RetryContextCache retryContextCache) {
    this.retryContextCache = retryContextCache;
  }

  /**
   * Setter for listeners. The listeners are executed before and after a retry block
   * (i.e. before and after all the attempts), and on an error (every attempt).
   *
   * @param listeners the {@link RetryListener}s
   * @see RetryListener
   */
  public void setListeners(RetryListener[] listeners) {
    this.listeners = listeners.clone();
  }

  /**
   * Register an additional listener at the end of the list.
   *
   * @param listener the {@link RetryListener}
   * @see #setListeners(RetryListener[])
   */
  public void registerListener(RetryListener listener) {
    registerListener(listener, this.listeners.length);
  }

  /**
   * Register an additional listener at the specified index.
   *
   * @param listener the {@link RetryListener}
   * @param index the position in the list.
   * @see #setListeners(RetryListener[])
   */
  public void registerListener(RetryListener listener, int index) {
    ArrayList<RetryListener> list = new ArrayList<>(Arrays.asList(this.listeners));
    if (index >= list.size()) {
      list.add(listener);
    }
    else {
      list.add(index, listener);
    }
    this.listeners = list.toArray(new RetryListener[0]);
  }

  /**
   * Return true if at least one listener is registered.
   *
   * @return true if listeners present.
   */
  public boolean hasListeners() {
    return this.listeners.length > 0;
  }

  /**
   * Setter for {@link BackOffPolicy}.
   *
   * @param backOffPolicy the {@link BackOffPolicy}
   */
  public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
    this.backOffPolicy = backOffPolicy;
  }

  /**
   * Setter for {@link RetryPolicy}.
   *
   * @param retryPolicy the {@link RetryPolicy}
   */
  public void setRetryPolicy(RetryPolicy retryPolicy) {
    this.retryPolicy = retryPolicy;
  }

  /**
   * Keep executing the callback until it either succeeds or the policy dictates that we
   * stop, in which case the most recent exception thrown by the callback will be
   * rethrown.
   *
   * @param retryCallback the {@link RetryCallback}
   * @throws TerminatedRetryException if the retry has been manually terminated by a
   * listener.
   * @see RetryOperations#execute(RetryCallback)
   */
  @Override
  public final <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E {
    return doExecute(retryCallback, null, null);
  }

  /**
   * Keep executing the callback until it either succeeds or the policy dictates that we
   * stop, in which case the recovery callback will be executed.
   *
   * @param retryCallback the {@link RetryCallback}
   * @param recoveryCallback the {@link RecoveryCallback}
   * @throws TerminatedRetryException if the retry has been manually terminated by a
   * listener.
   * @see RetryOperations#execute(RetryCallback, RecoveryCallback)
   */
  @Override
  public final <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback,
          RecoveryCallback<T> recoveryCallback) throws E {
    return doExecute(retryCallback, recoveryCallback, null);
  }

  /**
   * Execute the callback once if the policy dictates that we can, re-throwing any
   * exception encountered so that clients can re-present the same task later.
   *
   * @param retryCallback the {@link RetryCallback}
   * @param retryState the {@link RetryState}
   * @throws ExhaustedRetryException if the retry has been exhausted.
   * @see RetryOperations#execute(RetryCallback, RetryState)
   */
  @Override
  public final <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RetryState retryState)
          throws E, ExhaustedRetryException {
    return doExecute(retryCallback, null, retryState);
  }

  /**
   * Execute the callback once if the policy dictates that we can, re-throwing any
   * exception encountered so that clients can re-present the same task later.
   *
   * @param retryCallback the {@link RetryCallback}
   * @param recoveryCallback the {@link RecoveryCallback}
   * @param retryState the {@link RetryState}
   * @see RetryOperations#execute(RetryCallback, RetryState)
   */
  @Override
  public final <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback,
          RecoveryCallback<T> recoveryCallback, RetryState retryState) throws E, ExhaustedRetryException {
    return doExecute(retryCallback, recoveryCallback, retryState);
  }

  /**
   * Execute the callback once if the policy dictates that we can, otherwise execute the
   * recovery callback.
   *
   * @param recoveryCallback the {@link RecoveryCallback}
   * @param retryCallback the {@link RetryCallback}
   * @param state the {@link RetryState}
   * @param <T> the type of the return value
   * @param <E> the exception type to throw
   * @return T the retried value
   * @throws ExhaustedRetryException if the retry has been exhausted.
   * @throws E an exception if the retry operation fails
   * @see RetryOperations#execute(RetryCallback, RecoveryCallback, RetryState)
   */
  protected <T, E extends Throwable> T doExecute(RetryCallback<T, E> retryCallback,
          RecoveryCallback<T> recoveryCallback, RetryState state) throws E, ExhaustedRetryException {

    RetryPolicy retryPolicy = this.retryPolicy;
    BackOffPolicy backOffPolicy = this.backOffPolicy;

    // Allow the retry policy to initialise itself...
    RetryContext context = open(retryPolicy, state);
    if (logger.isTraceEnabled()) {
      logger.trace("RetryContext retrieved: {}", context);
    }

    // Make sure the context is available globally for clients who need
    // it...
    RetrySynchronizationManager.register(context);

    Throwable lastException = null;

    boolean exhausted = false;
    try {
      // Give clients a chance to enhance the context...
      boolean running = doOpenInterceptors(retryCallback, context);
      if (!running) {
        throw new TerminatedRetryException("Retry terminated abnormally by interceptor before first attempt");
      }

      // Get or Start the backoff context...
      BackOffContext backOffContext = null;
      Object resource = context.getAttribute("backOffContext");

      if (resource instanceof BackOffContext) {
        backOffContext = (BackOffContext) resource;
      }

      if (backOffContext == null) {
        backOffContext = backOffPolicy.start(context);
        if (backOffContext != null) {
          context.setAttribute("backOffContext", backOffContext);
        }
      }

      /*
       * We allow the whole loop to be skipped if the policy or context already
       * forbid the first try. This is used in the case of external retry to allow a
       * recovery in handleRetryExhausted without the callback processing (which
       * would throw an exception).
       */
      while (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {

        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Retry: count={}", context.getRetryCount());
          }
          // Reset the last exception, so if we are successful
          // the close interceptors will not think we failed...
          lastException = null;
          T result = retryCallback.doWithRetry(context);
          doOnSuccessInterceptors(retryCallback, context, result);
          return result;
        }
        catch (Throwable e) {
          lastException = e;

          try {
            registerThrowable(retryPolicy, state, context, e);
          }
          catch (Exception ex) {
            throw new TerminatedRetryException("Could not register throwable", ex);
          }
          finally {
            doOnErrorInterceptors(retryCallback, context, e);
          }

          if (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
            try {
              backOffPolicy.backOff(backOffContext);
            }
            catch (BackOffInterruptedException ex) {
              lastException = e;
              // back off was prevented by another thread - fail the retry
              if (logger.isDebugEnabled()) {
                logger.debug("Abort retry because interrupted: count={}", context.getRetryCount());
              }
              throw ex;
            }
          }

          if (logger.isDebugEnabled()) {
            logger.debug("Checking for rethrow: count={}", context.getRetryCount());
          }

          if (shouldRethrow(retryPolicy, context, state)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Rethrow in retry for policy: count={}", context.getRetryCount());
            }
            throw RetryTemplate.<E>wrapIfNecessary(e);
          }

        }

        /*
         * A stateful attempt that can retry may rethrow the exception before now,
         * but if we get this far in a stateful retry there's a reason for it,
         * like a circuit breaker or a rollback classifier.
         */
        if (state != null && context.hasAttribute(GLOBAL_STATE)) {
          break;
        }
      }

      if (state == null && logger.isDebugEnabled()) {
        logger.debug("Retry failed last attempt: count={}", context.getRetryCount());
      }

      exhausted = true;
      return handleRetryExhausted(recoveryCallback, context, state);

    }
    catch (Throwable e) {
      throw RetryTemplate.<E>wrapIfNecessary(e);
    }
    finally {
      close(retryPolicy, context, state, lastException == null || exhausted);
      doCloseInterceptors(retryCallback, context, lastException);
      RetrySynchronizationManager.clear();
    }

  }

  /**
   * Decide whether to proceed with the ongoing retry attempt. This method is called
   * before the {@link RetryCallback} is executed, but after the backoff and open
   * interceptors.
   *
   * @param retryPolicy the policy to apply
   * @param context the current retry context
   * @return true if we can continue with the attempt
   */
  protected boolean canRetry(RetryPolicy retryPolicy, RetryContext context) {
    return retryPolicy.canRetry(context);
  }

  /**
   * Clean up the cache if necessary and close the context provided (if the flag
   * indicates that processing was successful).
   *
   * @param retryPolicy the {@link RetryPolicy}
   * @param context the {@link RetryContext}
   * @param state the {@link RetryState}
   * @param succeeded whether the close succeeded
   */
  protected void close(RetryPolicy retryPolicy, RetryContext context, RetryState state, boolean succeeded) {
    if (state != null) {
      if (succeeded) {
        if (!context.hasAttribute(GLOBAL_STATE)) {
          retryContextCache.remove(state.getKey());
        }
        retryPolicy.close(context);
        context.setAttribute(RetryContext.CLOSED, true);
      }
    }
    else {
      retryPolicy.close(context);
      context.setAttribute(RetryContext.CLOSED, true);
    }
  }

  protected void registerThrowable(RetryPolicy retryPolicy, RetryState state, RetryContext context, Throwable e) {
    retryPolicy.registerThrowable(context, e);
    registerContext(context, state);
  }

  private void registerContext(RetryContext context, RetryState state) {
    if (state != null) {
      Object key = state.getKey();
      if (key != null) {
        if (context.getRetryCount() > 1 && !retryContextCache.containsKey(key)) {
          throw new RetryException("Inconsistent state for failed item key: cache key has changed. "
                  + "Consider whether equals() or hashCode() for the key might be inconsistent, "
                  + "or if you need to supply a better key");
        }
        retryContextCache.put(key, context);
      }
    }
  }

  /**
   * Delegate to the {@link RetryPolicy} having checked in the cache for an existing
   * value if the state is not null.
   *
   * @param state a {@link RetryState}
   * @param retryPolicy a {@link RetryPolicy} to delegate the context creation
   * @return a retry context, either a new one or the one used last time the same state
   * was encountered
   */
  protected RetryContext open(RetryPolicy retryPolicy, RetryState state) {

    if (state == null) {
      return doOpenInternal(retryPolicy);
    }

    Object key = state.getKey();
    if (state.isForceRefresh()) {
      return doOpenInternal(retryPolicy, state);
    }

    // If there is no cache hit we can avoid the possible expense of the
    // cache re-hydration.
    if (!retryContextCache.containsKey(key)) {
      // The cache is only used if there is a failure.
      return doOpenInternal(retryPolicy, state);
    }

    RetryContext context = retryContextCache.get(key);
    if (context == null) {
      if (retryContextCache.containsKey(key)) {
        throw new RetryException("Inconsistent state for failed item: no history found. "
                + "Consider whether equals() or hashCode() for the item might be inconsistent, "
                + "or if you need to supply a better ItemKeyGenerator");
      }
      // The cache could have been expired in between calls to
      // containsKey(), so we have to live with this:
      return doOpenInternal(retryPolicy, state);
    }

    // Start with a clean slate for state that others may be inspecting
    context.removeAttribute(RetryContext.CLOSED);
    context.removeAttribute(RetryContext.EXHAUSTED);
    context.removeAttribute(RetryContext.RECOVERED);
    return context;

  }

  private RetryContext doOpenInternal(RetryPolicy retryPolicy, RetryState state) {
    RetryContext context = retryPolicy.open(RetrySynchronizationManager.getContext());
    if (state != null) {
      context.setAttribute(RetryContext.STATE_KEY, state.getKey());
    }
    if (context.hasAttribute(GLOBAL_STATE)) {
      registerContext(context, state);
    }
    return context;
  }

  private RetryContext doOpenInternal(RetryPolicy retryPolicy) {
    return doOpenInternal(retryPolicy, null);
  }

  /**
   * Actions to take after final attempt has failed. If there is state clean up the
   * cache. If there is a recovery callback, execute that and return its result.
   * Otherwise throw an exception.
   *
   * @param recovery the callback for recovery (might be null)
   * @param context the current retry context
   * @param state the {@link RetryState}
   * @param <T> the type to classify
   * @return T the payload to return
   * @throws Exception if the callback does, and if there is no callback and the state
   * is null then the last exception from the context
   * @throws ExhaustedRetryException if the state is not null and there is no recovery
   * callback
   * @throws Throwable if there is an error
   */
  protected <T> T handleRetryExhausted(RecoveryCallback<T> recovery,
          RetryContext context, RetryState state) throws Throwable {
    context.setAttribute(RetryContext.EXHAUSTED, true);
    if (state != null && !context.hasAttribute(GLOBAL_STATE)) {
      this.retryContextCache.remove(state.getKey());
    }
    boolean doRecover = !Boolean.TRUE.equals(context.getAttribute(RetryContext.NO_RECOVERY));
    if (recovery != null) {
      if (doRecover) {
        T recovered = recovery.recover(context);
        context.setAttribute(RetryContext.RECOVERED, true);
        return recovered;
      }
      else {
        logger.debug("Retry exhausted and recovery disabled for this throwable");
      }
    }
    if (state != null) {
      this.logger.debug("Retry exhausted after last attempt with no recovery path.");
      rethrow(context, "Retry exhausted after last attempt with no recovery path",
              this.throwLastExceptionOnExhausted || !doRecover);
    }
    throw wrapIfNecessary(context.getLastThrowable());
  }

  protected <E extends Throwable> void rethrow(RetryContext context, String message, boolean wrap) throws E {
    if (wrap) {
      @SuppressWarnings("unchecked")
      E rethrow = (E) context.getLastThrowable();
      throw rethrow;
    }
    else {
      throw new ExhaustedRetryException(message, context.getLastThrowable());
    }
  }

  /**
   * Extension point for subclasses to decide on behaviour after catching an exception
   * in a {@link RetryCallback}. Normal stateless behaviour is not to rethrow, and if
   * there is state we rethrow.
   *
   * @param retryPolicy the retry policy
   * @param context the current context
   * @param state the current retryState
   * @return true if the state is not null but subclasses might choose otherwise
   */
  protected boolean shouldRethrow(RetryPolicy retryPolicy, RetryContext context, RetryState state) {
    return state != null && state.rollbackFor(context.getLastThrowable());
  }

  private <T, E extends Throwable> boolean doOpenInterceptors(RetryCallback<T, E> callback, RetryContext context) {
    boolean result = true;
    for (RetryListener listener : listeners) {
      result = result && listener.open(context, callback);
    }
    return result;
  }

  private <T, E extends Throwable> void doCloseInterceptors(
          RetryCallback<T, E> callback, RetryContext context, Throwable lastException) {
    for (int i = listeners.length; i-- > 0; ) {
      listeners[i].close(context, callback, lastException);
    }
  }

  private <T, E extends Throwable> void doOnSuccessInterceptors(
          RetryCallback<T, E> callback, RetryContext context, T result) {
    for (int i = listeners.length; i-- > 0; ) {
      listeners[i].onSuccess(context, callback, result);
    }
  }

  private <T, E extends Throwable> void doOnErrorInterceptors(
          RetryCallback<T, E> callback, RetryContext context, Throwable throwable) {
    for (int i = listeners.length; i-- > 0; ) {
      listeners[i].onError(context, callback, throwable);
    }
  }

  /**
   * Re-throws the original throwable if it is an Exception, and wraps non-exceptions
   * into {@link RetryException}.
   */
  private static <E extends Throwable> E wrapIfNecessary(Throwable throwable) throws RetryException {
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
    else if (throwable instanceof Exception) {
      @SuppressWarnings("unchecked")
      E rethrow = (E) throwable;
      return rethrow;
    }
    else {
      throw new RetryException("Exception in retry", throwable);
    }
  }

}
