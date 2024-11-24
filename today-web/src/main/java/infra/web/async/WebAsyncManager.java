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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.util.DisconnectedClientHelper;

/**
 * The central class for managing asynchronous request processing, mainly intended
 * as an SPI and not typically used directly by application classes.
 *
 * <p>An async scenario starts with request processing as usual in a thread (T1).
 * Concurrent request handling can be initiated by calling
 * {@link #startCallableProcessing(Callable, Object...) startCallableProcessing} or
 * {@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing},
 * both of which produce a result in a separate thread (T2). The result is saved
 * and the request dispatched to the container, to resume processing with the saved
 * result in a third thread (T3). Within the dispatched thread (T3), the saved
 * result can be accessed via {@link #getConcurrentResult()} or its presence
 * detected via {@link #hasConcurrentResult()}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebAsyncManager {

  /**
   * The name attribute of the {@link RequestContext}.
   */
  public static final String WEB_ASYNC_REQUEST_ATTRIBUTE = WebAsyncManager.class.getName() + ".WEB_REQUEST";

  /**
   * The name attribute containing the result.
   */
  public static final String WEB_ASYNC_RESULT_ATTRIBUTE = WebAsyncManager.class.getName() + ".WEB_ASYNC_RESULT";

  private static final Object RESULT_NONE = new Object();

  /**
   * Log category to use for network failure after a client has gone away.
   *
   * @see DisconnectedClientHelper
   */
  private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
          "infra.web.server.DisconnectedClient";

  private static final DisconnectedClientHelper disconnectedClientHelper =
          new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);

  private static final AsyncTaskExecutor DEFAULT_TASK_EXECUTOR =
          new SimpleAsyncTaskExecutor(WebAsyncManager.class.getSimpleName());

  private static final Logger logger = LoggerFactory.getLogger(WebAsyncManager.class);

  private static final TimeoutAsyncProcessingInterceptor timeoutInterceptor =
          new TimeoutAsyncProcessingInterceptor();

  @Nullable
  private Long asyncRequestTimeout;

  private AsyncTaskExecutor taskExecutor = DEFAULT_TASK_EXECUTOR;

  @Nullable
  private volatile Object concurrentResult = RESULT_NONE;

  @Nullable
  private volatile Object[] concurrentResultContext;

  /*
   * Whether the concurrentResult is an error.
   */
  private volatile boolean errorHandlingInProgress;

  @Nullable
  private LinkedHashMap<Object, CallableProcessingInterceptor> callableInterceptors;

  @Nullable
  private LinkedHashMap<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors;

  private final RequestContext requestContext;

  public WebAsyncManager(RequestContext requestContext) {
    this.requestContext = requestContext;
  }

  /**
   * Configure an AsyncTaskExecutor for use with concurrent processing via
   * {@link #startCallableProcessing(Callable, Object...)}.
   * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Set the time required for concurrent handling to complete.
   *
   * @param timeout amount of time in milliseconds; {@code null} means no
   * timeout, i.e. rely on the default timeout of the container.
   */
  public void setTimeout(@Nullable Long timeout) {
    this.asyncRequestTimeout = timeout;
  }

  /**
   * Whether a result value exists as a result of concurrent handling.
   */
  public boolean hasConcurrentResult() {
    return concurrentResult != RESULT_NONE;
  }

  /**
   * Provides access to the result from concurrent handling.
   *
   * @return an Object, possibly an {@code Exception} or {@code Throwable} if
   * concurrent handling raised one.
   * @see #clearConcurrentResult()
   */
  @Nullable
  public Object getConcurrentResult() {
    return concurrentResult;
  }

  /**
   * Provides access to additional processing context saved at the start of
   * concurrent handling.
   *
   * @see #clearConcurrentResult()
   */
  @Nullable
  public Object[] getConcurrentResultContext() {
    return concurrentResultContext;
  }

  /**
   * Get the {@link CallableProcessingInterceptor} registered under the given key.
   *
   * @param key the key
   * @return the interceptor registered under that key, or {@code null} if none
   */
  @Nullable
  public CallableProcessingInterceptor getCallableInterceptor(Object key) {
    return callableInterceptors == null ? null : callableInterceptors.get(key);
  }

  /**
   * Get the {@link DeferredResultProcessingInterceptor} registered under the given key.
   *
   * @param key the key
   * @return the interceptor registered under that key, or {@code null} if none
   */
  @Nullable
  public DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
    return deferredResultInterceptors == null ? null : deferredResultInterceptors.get(key);
  }

  /**
   * Register a {@link CallableProcessingInterceptor} under the given key.
   *
   * @param key the key
   * @param interceptor the interceptor to register
   */
  public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
    Assert.notNull(key, "Key is required");
    Assert.notNull(interceptor, "CallableProcessingInterceptor is required");
    if (callableInterceptors == null) {
      callableInterceptors = new LinkedHashMap<>();
    }
    callableInterceptors.put(key, interceptor);
  }

  /**
   * Register a {@link CallableProcessingInterceptor} without a key.
   * The key is derived from the class name and hashcode.
   *
   * @param interceptors one or more interceptors to register
   */
  public void registerCallableInterceptors(@Nullable List<CallableProcessingInterceptor> interceptors) {
    if (interceptors != null) {
      if (callableInterceptors == null) {
        callableInterceptors = new LinkedHashMap<>();
      }
      for (CallableProcessingInterceptor interceptor : interceptors) {
        String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
        callableInterceptors.put(key, interceptor);
      }
    }
  }

  /**
   * Register a {@link DeferredResultProcessingInterceptor} under the given key.
   *
   * @param key the key
   * @param interceptor the interceptor to register
   */
  public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
    Assert.notNull(key, "Key is required");
    Assert.notNull(interceptor, "DeferredResultProcessingInterceptor is required");
    if (deferredResultInterceptors == null) {
      deferredResultInterceptors = new LinkedHashMap<>();
    }
    deferredResultInterceptors.put(key, interceptor);
  }

  /**
   * Register one or more {@link DeferredResultProcessingInterceptor DeferredResultProcessingInterceptors} without a specified key.
   * The default key is derived from the interceptor class name and hash code.
   *
   * @param interceptors one or more interceptors to register
   */
  public void registerDeferredResultInterceptors(@Nullable List<DeferredResultProcessingInterceptor> interceptors) {
    if (interceptors != null) {
      if (deferredResultInterceptors == null) {
        deferredResultInterceptors = new LinkedHashMap<>();
      }
      for (DeferredResultProcessingInterceptor interceptor : interceptors) {
        String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
        deferredResultInterceptors.put(key, interceptor);
      }
    }
  }

  /**
   * Clear {@linkplain #getConcurrentResult() concurrentResult} and
   * {@linkplain #getConcurrentResultContext() concurrentResultContext}.
   */
  public void clearConcurrentResult() {
    synchronized(WebAsyncManager.this) {
      this.concurrentResult = RESULT_NONE;
      this.concurrentResultContext = null;
    }
  }

  /**
   * Start concurrent request processing and execute the given task with an
   * {@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor}. The result
   * from the task execution is saved and the request dispatched in order to
   * resume processing of that result. If the task raises an Exception then
   * the saved result will be the raised Exception.
   *
   * @param callable a unit of work to be executed asynchronously
   * @param processingContext additional context to save that can be accessed
   * via {@link #getConcurrentResultContext()}
   * @throws Exception if concurrent processing failed to start
   * @see #getConcurrentResult()
   * @see #getConcurrentResultContext()
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void startCallableProcessing(Callable<?> callable, Object... processingContext) throws Exception {
    Assert.notNull(callable, "Callable is required");
    startCallableProcessing(new WebAsyncTask(callable), processingContext);
  }

  /**
   * Use the given {@link WebAsyncTask} to configure the task executor as well as
   * the timeout value of the {@code AsyncWebRequest} before delegating to
   * {@link #startCallableProcessing(Callable, Object...)}.
   *
   * @param webAsyncTask a WebAsyncTask containing the target {@code Callable}
   * @param processingContext additional context to save that can be accessed
   * via {@link #getConcurrentResultContext()}
   * @throws Exception if concurrent processing failed to start
   */
  public void startCallableProcessing(WebAsyncTask<?> webAsyncTask, Object... processingContext) throws Exception {
    Assert.notNull(webAsyncTask, "WebAsyncTask is required");
    AsyncWebRequest asyncRequest = getAsyncWebRequest();

    Long timeout = webAsyncTask.getTimeout();
    if (timeout == null) {
      timeout = this.asyncRequestTimeout;
    }
    if (timeout != null) {
      asyncRequest.setTimeout(timeout);
    }

    AsyncTaskExecutor executor = webAsyncTask.getExecutor();
    if (executor != null) {
      this.taskExecutor = executor;
    }

    var interceptors = new ArrayList<CallableProcessingInterceptor>(2);
    interceptors.add(webAsyncTask.createInterceptor());
    if (callableInterceptors != null) {
      interceptors.addAll(callableInterceptors.values());
    }
    interceptors.add(timeoutInterceptor);

    Callable<?> callable = webAsyncTask.getCallable();
    var interceptorChain = new CallableInterceptorChain(interceptors);
    asyncRequest.addTimeoutHandler(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("Async request timeout for {}", formatRequestUri());
      }
      Object result = interceptorChain.triggerAfterTimeout(requestContext, callable);
      if (result != CallableProcessingInterceptor.RESULT_NONE) {
        setConcurrentResultAndDispatch(result);
      }
    });

    asyncRequest.addErrorHandler(ex -> {
      if (!errorHandlingInProgress) {
        if (logger.isDebugEnabled()) {
          logger.debug("Async request error for {}: {}", formatRequestUri(), ex);
        }
        Object result = interceptorChain.triggerAfterError(requestContext, callable, ex);
        result = result != CallableProcessingInterceptor.RESULT_NONE ? result : ex;
        setConcurrentResultAndDispatch(result);
      }
    });

    asyncRequest.addCompletionHandler(
            () -> interceptorChain.triggerAfterCompletion(requestContext, callable));

    interceptorChain.applyBeforeConcurrentHandling(requestContext, callable);
    startAsyncProcessing(asyncRequest, processingContext);
    try {
      Future<?> future = taskExecutor.submit(() -> {
        // context aware
        RequestContextHolder.set(requestContext);
        Object result = null;
        try {
          interceptorChain.applyPreProcess(requestContext, callable);
          result = callable.call();
        }
        catch (Throwable ex) {
          result = ex;
        }
        finally {
          result = interceptorChain.applyPostProcess(requestContext, callable, result);
        }
        setConcurrentResultAndDispatch(result);
        RequestContextHolder.cleanup();
      });
      interceptorChain.setTaskFuture(future);
    }
    catch (Throwable ex) {
      Object result = interceptorChain.applyPostProcess(requestContext, callable, ex);
      setConcurrentResultAndDispatch(result);
    }
  }

  private AsyncWebRequest getAsyncWebRequest() {
    return requestContext.getAsyncWebRequest();
  }

  private String formatRequestUri() {
    return requestContext.getRequestURI();
  }

  private void setConcurrentResultAndDispatch(@Nullable Object result) {
    synchronized(this) {
      if (concurrentResult != RESULT_NONE) {
        return;
      }
      this.concurrentResult = result;
      this.errorHandlingInProgress = (result instanceof Throwable);
    }

    AsyncWebRequest asyncRequest = getAsyncWebRequest();
    if (asyncRequest.isAsyncComplete()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Async result set but request already complete: {}", formatRequestUri());
      }
      return;
    }

    if (result instanceof Exception ex && disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Async %s, dispatch to %s".formatted(this.errorHandlingInProgress ? "error" : "result set", formatRequestUri()));
    }
    asyncRequest.dispatch(result);
  }

  /**
   * Start concurrent request processing and initialize the given
   * {@link DeferredResult} with a {@link DeferredResultHandler} that saves
   * the result and dispatches the request to resume processing of that
   * result. The {@code AsyncWebRequest} is also updated with a completion
   * handler that expires the {@code DeferredResult} and a timeout handler
   * assuming the {@code DeferredResult} has a default timeout result.
   *
   * @param deferred the DeferredResult instance to initialize
   * @param processingContext additional context to save that can be accessed
   * via {@link #getConcurrentResultContext()}
   * @throws Exception if concurrent processing failed to start
   * @see #getConcurrentResult()
   * @see #getConcurrentResultContext()
   */
  public void startDeferredResultProcessing(DeferredResult<?> deferred, Object... processingContext) throws Exception {
    Assert.notNull(deferred, "DeferredResult is required");
    AsyncWebRequest asyncRequest = getAsyncWebRequest();
    Long timeout = deferred.getTimeoutValue();
    if (timeout == null) {
      timeout = this.asyncRequestTimeout;
    }

    if (timeout != null) {
      asyncRequest.setTimeout(timeout);
    }

    ArrayList<DeferredResultProcessingInterceptor> interceptors = new ArrayList<>(2);
    interceptors.add(deferred.createInterceptor());
    if (deferredResultInterceptors != null) {
      interceptors.addAll(deferredResultInterceptors.values());
    }
    interceptors.add(timeoutInterceptor);

    var chain = new DeferredResultInterceptorChain(interceptors);
    asyncRequest.addTimeoutHandler(() -> {
      try {
        chain.triggerAfterTimeout(requestContext, deferred);
      }
      catch (Throwable ex) {
        setConcurrentResultAndDispatch(ex);
      }
    });

    asyncRequest.addErrorHandler(ex -> {
      if (!errorHandlingInProgress) {
        try {
          if (!chain.triggerAfterError(requestContext, deferred, ex)) {
            return;
          }
          deferred.setErrorResult(ex);
        }
        catch (Throwable interceptorEx) {
          setConcurrentResultAndDispatch(interceptorEx);
        }
      }
    });

    asyncRequest.addCompletionHandler(() ->
            chain.triggerAfterCompletion(requestContext, deferred));

    chain.applyBeforeConcurrentHandling(requestContext, deferred);
    startAsyncProcessing(asyncRequest, processingContext);

    try {
      chain.applyPreProcess(requestContext, deferred);
      deferred.setResultHandler(result -> {
        result = chain.applyPostProcess(requestContext, deferred, result);
        setConcurrentResultAndDispatch(result);
      });
    }
    catch (Throwable ex) {
      setConcurrentResultAndDispatch(ex);
    }
  }

  private void startAsyncProcessing(AsyncWebRequest asyncRequest, Object[] processingContext) {
    synchronized(WebAsyncManager.this) {
      this.concurrentResult = RESULT_NONE;
      this.concurrentResultContext = processingContext;
      this.errorHandlingInProgress = false;
    }
    asyncRequest.startAsync();

    if (logger.isDebugEnabled()) {
      logger.debug("Started async request");
    }
  }

  //

  @Nullable
  public static Object findHttpRequestHandler(RequestContext request) {
    var asyncManager = request.getAsyncManager();
    Object[] concurrentResultContext = asyncManager.getConcurrentResultContext();
    if (concurrentResultContext != null && concurrentResultContext.length == 1) {
      return concurrentResultContext[0];
    }
    else {
      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        return matchingMetadata.getHandler();
      }
    }
    return null;
  }

}
