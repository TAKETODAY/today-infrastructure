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

package cn.taketoday.web.context.async;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

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
  public abstract void dispatch(Object concurrentResult);

}
