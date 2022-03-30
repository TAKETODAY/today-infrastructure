/*
 * Copyright 2002-2021 the original author or authors.
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

package cn.taketoday.web.context.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
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

  private final RequestContext requestContext;

  public StandardServletAsyncWebRequest(RequestContext requestContext) {
    this.requestContext = requestContext;
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
    return (this.asyncContext != null && ServletUtils.getServletRequest(requestContext).isAsyncStarted());
  }

  /**
   * Whether async request processing has completed.
   * <p>It is important to avoid use of request and response objects after async
   * processing has completed. Servlet containers often re-use them.
   */
  @Override
  public boolean isAsyncComplete() {
    return this.asyncCompleted.get();
  }

  @Override
  public RequestContext getRequestContext() {
    return requestContext;
  }

  @Override
  public void startAsync() {
    HttpServletRequest servletRequest = ServletUtils.getServletRequest(requestContext);
    Assert.state(servletRequest.isAsyncSupported(),
            "Async support must be enabled on a servlet and for all filters involved " +
                    "in async request processing. This is done in Java code using the Servlet API " +
                    "or by adding \"<async-supported>true</async-supported>\" to servlet and " +
                    "filter declarations in web.xml.");
    Assert.state(!isAsyncComplete(), "Async processing has already completed");

    if (isAsyncStarted()) {
      return;
    }
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(requestContext);
    this.asyncContext = servletRequest.startAsync(servletRequest, servletResponse);
    this.asyncContext.addListener(this);
    if (this.timeout != null) {
      this.asyncContext.setTimeout(this.timeout);
    }
  }

  @Override
  public void dispatch() {
    Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
    this.asyncContext.dispatch();
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
    this.exceptionHandlers.forEach(consumer -> consumer.accept(event.getThrowable()));
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {
    this.timeoutHandlers.forEach(Runnable::run);
  }

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    this.completionHandlers.forEach(Runnable::run);
    this.asyncContext = null;
    this.asyncCompleted.set(true);
  }

}
