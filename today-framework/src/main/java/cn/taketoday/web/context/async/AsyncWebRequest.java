/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.context.async;

import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Extends {@link RequestContext} with methods for asynchronous request processing.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface AsyncWebRequest {

  /**
   * Set the time required for concurrent handling to complete.
   * This property should not be set when concurrent handling is in progress,
   * i.e. when {@link #isAsyncStarted()} is {@code true}.
   *
   * @param timeout amount of time in milliseconds; {@code null} means no
   * timeout, i.e. rely on the default timeout of the container.
   */
  void setTimeout(@Nullable Long timeout);

  /**
   * Add a handler to invoke when concurrent handling has timed out.
   */
  void addTimeoutHandler(Runnable runnable);

  /**
   * Add a handler to invoke when an error occurred while concurrent
   * handling of a request.
   */
  void addErrorHandler(Consumer<Throwable> exceptionHandler);

  /**
   * Add a handler to invoke when request processing completes.
   */
  void addCompletionHandler(Runnable runnable);

  /**
   * Mark the start of asynchronous request processing so that when the main
   * processing thread exits, the response remains open for further processing
   * in another thread.
   *
   * @throws IllegalStateException if async processing has completed or is not supported
   */
  void startAsync();

  /**
   * Whether the request is in async mode following a call to {@link #startAsync()}.
   * Returns "false" if asynchronous processing never started, has completed,
   * or the request was dispatched for further processing.
   */
  boolean isAsyncStarted();

  /**
   * Dispatch the request to the container in order to resume processing after
   * concurrent execution in an application thread.
   */
  void dispatch();

  /**
   * Whether asynchronous processing has completed.
   */
  boolean isAsyncComplete();

}
