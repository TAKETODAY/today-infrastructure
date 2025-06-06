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

package infra.http.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.AsyncContext;
import infra.mock.api.AsyncEvent;
import infra.mock.api.AsyncListener;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;

/**
 * A {@link ServerHttpAsyncRequestControl} to use on Servlet containers (Servlet 3.0+).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockServerHttpAsyncRequestControl implements ServerHttpAsyncRequestControl, AsyncListener {

  private static final long NO_TIMEOUT_VALUE = Long.MIN_VALUE;

  @Nullable
  private AsyncContext asyncContext;

  private final MockServerHttpRequest request;
  private final MockServerHttpResponse response;
  private final AtomicBoolean asyncCompleted = new AtomicBoolean();

  /**
   * Constructor accepting a request and response pair that are expected to be of type
   * {@link MockServerHttpRequest} and {@link MockServerHttpResponse}
   * respectively.
   */
  public MockServerHttpAsyncRequestControl(
          MockServerHttpRequest request, MockServerHttpResponse response) {
    Assert.notNull(request, "request is required");
    Assert.notNull(response, "response is required");

    Assert.isTrue(request.getRequest().isAsyncSupported(),
            "Async support must be enabled on a servlet and for all filters involved " +
                    "in async request processing. This is done in Java code using the Servlet API " +
                    "or by adding \"<async-supported>true</async-supported>\" to servlet and " +
                    "filter declarations in web.xml. Also you must use a Servlet 3.0+ container");
    this.request = request;
    this.response = response;
  }

  @Override
  public boolean isStarted() {
    return (this.asyncContext != null && this.request.getRequest().isAsyncStarted());
  }

  @Override
  public boolean isCompleted() {
    return this.asyncCompleted.get();
  }

  @Override
  public void start() {
    start(NO_TIMEOUT_VALUE);
  }

  @Override
  public void start(long timeout) {
    Assert.state(!isCompleted(), "Async processing has already completed");
    if (isStarted()) {
      return;
    }

    HttpMockRequest servletRequest = this.request.getRequest();
    HttpMockResponse servletResponse = this.response.getResponse();

    this.asyncContext = servletRequest.startAsync(servletRequest, servletResponse);
    this.asyncContext.addListener(this);

    if (timeout != NO_TIMEOUT_VALUE) {
      this.asyncContext.setTimeout(timeout);
    }
  }

  @Override
  public void complete() {
    if (this.asyncContext != null && isStarted() && !isCompleted()) {
      this.asyncContext.complete();
    }
  }

  // ---------------------------------------------------------------------
  // Implementation of AsyncListener methods
  // ---------------------------------------------------------------------

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    this.asyncContext = null;
    this.asyncCompleted.set(true);
  }

  @Override
  public void onStartAsync(AsyncEvent event) throws IOException { }

  @Override
  public void onError(AsyncEvent event) throws IOException { }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException { }

}
