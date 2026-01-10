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

package infra.web.async;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Extends {@link RequestContext} with methods for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AsyncWebRequest {

  @Nullable
  protected Long timeout;

  protected final AtomicBoolean asyncCompleted = new AtomicBoolean();

  protected final ArrayList<Runnable> timeoutHandlers = new ArrayList<>();

  protected final ArrayList<Runnable> completionHandlers = new ArrayList<>();

  protected final ArrayList<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

  /**
   * Set the time required for concurrent handling to complete.
   * This property should not be set when concurrent handling is in progress,
   * i.e. when {@link #isAsyncStarted()} is {@code true}.
   *
   * @param timeout amount of time in milliseconds; {@code null} means no
   * timeout, i.e. rely on the default timeout of the container.
   */
  public void setTimeout(@Nullable Long timeout) {
    Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
    this.timeout = timeout;
  }

  /**
   * Add a handler to invoke when concurrent handling has timed out.
   */
  public void addTimeoutHandler(Runnable timeoutHandler) {
    this.timeoutHandlers.add(timeoutHandler);
  }

  /**
   * Add a handler to invoke when an error occurred while concurrent
   * handling of a request.
   */
  public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
    this.exceptionHandlers.add(exceptionHandler);
  }

  /**
   * Add a handler to invoke when request processing completes.
   */
  public void addCompletionHandler(Runnable runnable) {
    this.completionHandlers.add(runnable);
  }

  /**
   * Whether asynchronous processing has completed.
   */
  public boolean isAsyncComplete() {
    return asyncCompleted.get();
  }

  protected final void dispatchEvent(ArrayList<Runnable> handler) {
    for (Runnable runnable : handler) {
      runnable.run();
    }
  }

  /**
   * Mark the start of asynchronous request processing so that when the main
   * processing thread exits, the response remains open for further processing
   * in another thread.
   *
   * @throws IllegalStateException if async processing has completed or is not supported
   */
  public abstract void startAsync();

  /**
   * Whether the request is in async mode following a call to {@link #startAsync()}.
   * Returns "false" if asynchronous processing never started, has completed,
   * or the request was dispatched for further processing.
   */
  public abstract boolean isAsyncStarted();

  /**
   * Dispatch the request to the container in order to resume processing after
   * concurrent execution in an application thread.
   */
  public abstract void dispatch(@Nullable Object concurrentResult);

}
