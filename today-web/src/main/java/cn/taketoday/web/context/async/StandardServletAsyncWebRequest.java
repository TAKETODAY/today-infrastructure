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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A Servlet implementation of {@link AsyncWebRequest}.
 *
 * <p>The servlet and all filters involved in an async request must have async
 * support enabled using the Servlet API or by adding an
 * <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code> element to servlet and filter
 * declarations in {@code web.xml}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardServletAsyncWebRequest implements AsyncWebRequest, AsyncListener {

  private Long timeout;

  private AsyncContext asyncContext;

  private final AtomicBoolean asyncCompleted = new AtomicBoolean();

  private final List<Runnable> timeoutHandlers = new ArrayList<>();

  private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

  private final List<Runnable> completionHandlers = new ArrayList<>();

  private final HttpServletRequest servletRequest;

  private final HttpServletResponse servletResponse;

  StandardServletAsyncWebRequest(ServletRequestContext context) {
    this.servletRequest = context.getRequest();
    this.servletResponse = context.getResponse();
  }

  /**
   * Create a new instance for the given request/response pair.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   */
  public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
    this.servletRequest = request;
    this.servletResponse = response;
  }

  /**
   * In Servlet 3 async processing, the timeout period begins after the
   * container processing thread has exited.
   */
  @Override
  public void setTimeout(Long timeout) {
    Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
    this.timeout = timeout;
  }

  @Override
  public void addTimeoutHandler(Runnable timeoutHandler) {
    this.timeoutHandlers.add(timeoutHandler);
  }

  @Override
  public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
    this.exceptionHandlers.add(exceptionHandler);
  }

  @Override
  public void addCompletionHandler(Runnable runnable) {
    this.completionHandlers.add(runnable);
  }

  @Override
  public boolean isAsyncStarted() {
    return asyncContext != null && servletRequest.isAsyncStarted();
  }

  /**
   * Whether async request processing has completed.
   * <p>It is important to avoid use of request and response objects after async
   * processing has completed. Servlet containers often re-use them.
   */
  @Override
  public boolean isAsyncComplete() {
    return asyncCompleted.get();
  }

  @Override
  public void startAsync() {
    Assert.state(servletRequest.isAsyncSupported(),
            "Async support must be enabled on a servlet and for all filters involved " +
                    "in async request processing. This is done in Java code using the Servlet API " +
                    "or by adding \"<async-supported>true</async-supported>\" to servlet and " +
                    "filter declarations in web.xml.");
    Assert.state(!isAsyncComplete(), "Async processing has already completed");

    if (isAsyncStarted()) {
      return;
    }
    this.asyncContext = servletRequest.startAsync(servletRequest, servletResponse);
    asyncContext.addListener(this);
    if (timeout != null) {
      asyncContext.setTimeout(this.timeout);
    }
  }

  @Override
  public void dispatch() {
    Assert.notNull(asyncContext, "Cannot dispatch without an AsyncContext");
    asyncContext.dispatch();
  }

  // ---------------------------------------------------------------------
  // Implementation of AsyncListener methods
  // ---------------------------------------------------------------------

  @Override
  public void onStartAsync(AsyncEvent event) throws IOException {
    // no-op
  }

  @Override
  public void onError(AsyncEvent event) throws IOException {
    exceptionHandlers.forEach(consumer -> consumer.accept(event.getThrowable()));
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {
    timeoutHandlers.forEach(Runnable::run);
  }

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    completionHandlers.forEach(Runnable::run);
    this.asyncContext = null;
    asyncCompleted.set(true);
  }

}
