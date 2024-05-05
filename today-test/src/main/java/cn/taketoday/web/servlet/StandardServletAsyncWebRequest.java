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

package cn.taketoday.web.servlet;

import java.io.IOException;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.async.AsyncWebRequest;
import cn.taketoday.web.async.WebAsyncManager;
import cn.taketoday.mock.api.AsyncContext;
import cn.taketoday.mock.api.AsyncEvent;
import cn.taketoday.mock.api.AsyncListener;
import cn.taketoday.mock.api.http.HttpServletRequest;
import cn.taketoday.mock.api.http.HttpServletResponse;

/**
 * A Servlet implementation of {@link AsyncWebRequest}.
 *
 * <p>The servlet and all filters involved in an async request must have async
 * support enabled using the Servlet API or by adding an
 * <code>&lt;async-supported&gt;true&lt;/async-supported&gt;</code> element to servlet and filter
 * declarations in {@code web.xml}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardServletAsyncWebRequest extends AsyncWebRequest implements AsyncListener {

  @Nullable
  private AsyncContext asyncContext;

  private final ServletRequestContext request;

  private final HttpServletRequest servletRequest;

  private final HttpServletResponse servletResponse;

  public StandardServletAsyncWebRequest(ServletRequestContext context) {
    this.request = context;
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
    this.request = new ServletRequestContext(null, request, response);
    this.servletRequest = request;
    this.servletResponse = response;
  }

  @Override
  public boolean isAsyncStarted() {
    return asyncContext != null && servletRequest.isAsyncStarted();
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
  public void dispatch(Object concurrentResult) {
    Assert.notNull(asyncContext, "Cannot dispatch without an AsyncContext");

    servletRequest.setAttribute(WebAsyncManager.WEB_ASYNC_REQUEST_ATTRIBUTE, request);
    servletRequest.setAttribute(WebAsyncManager.WEB_ASYNC_RESULT_ATTRIBUTE, concurrentResult);

    asyncContext.dispatch();
  }

  // ---------------------------------------------------------------------
  // Implementation of AsyncListener methods
  // ---------------------------------------------------------------------

  @Override
  public void onStartAsync(AsyncEvent event) {
    // no-op
  }

  @Override
  public void onError(AsyncEvent event) throws IOException {
    Throwable throwable = event.getThrowable();
    for (Consumer<Throwable> exceptionHandler : exceptionHandlers) {
      exceptionHandler.accept(throwable);
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {
    dispatchEvent(timeoutHandlers);
  }

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    dispatchEvent(completionHandlers);
    this.asyncContext = null;
    asyncCompleted.set(true);
  }

}
