/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import cn.taketoday.web.mock.http.HttpServletRequest;
import cn.taketoday.web.mock.http.HttpServletResponse;

/**
 * A {@link ServerHttpAsyncRequestControl} to use on Servlet containers (Servlet 3.0+).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletServerHttpAsyncRequestControl implements ServerHttpAsyncRequestControl, AsyncListener {

  private static final long NO_TIMEOUT_VALUE = Long.MIN_VALUE;

  @Nullable
  private AsyncContext asyncContext;

  private final ServletServerHttpRequest request;
  private final ServletServerHttpResponse response;
  private final AtomicBoolean asyncCompleted = new AtomicBoolean();

  /**
   * Constructor accepting a request and response pair that are expected to be of type
   * {@link ServletServerHttpRequest} and {@link ServletServerHttpResponse}
   * respectively.
   */
  public ServletServerHttpAsyncRequestControl(
          ServletServerHttpRequest request, ServletServerHttpResponse response) {
    Assert.notNull(request, "request is required");
    Assert.notNull(response, "response is required");

    Assert.isTrue(request.getServletRequest().isAsyncSupported(),
            "Async support must be enabled on a servlet and for all filters involved " +
                    "in async request processing. This is done in Java code using the Servlet API " +
                    "or by adding \"<async-supported>true</async-supported>\" to servlet and " +
                    "filter declarations in web.xml. Also you must use a Servlet 3.0+ container");
    this.request = request;
    this.response = response;
  }

  @Override
  public boolean isStarted() {
    return (this.asyncContext != null && this.request.getServletRequest().isAsyncStarted());
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

    HttpServletRequest servletRequest = this.request.getServletRequest();
    HttpServletResponse servletResponse = this.response.getServletResponse();

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
