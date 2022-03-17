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

package cn.taketoday.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.WebUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mock implementation of the {@link AsyncContext} interface.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockAsyncContext implements AsyncContext {

  private final HttpServletRequest request;

  @Nullable
  private final HttpServletResponse response;

  private final List<AsyncListener> listeners = new ArrayList<>();

  @Nullable
  private String dispatchedPath;

  private long timeout = 10 * 1000L;

  private final List<Runnable> dispatchHandlers = new ArrayList<>();

  public MockAsyncContext(ServletRequest request, @Nullable ServletResponse response) {
    this.request = (HttpServletRequest) request;
    this.response = (HttpServletResponse) response;
  }

  public void addDispatchHandler(Runnable handler) {
    Assert.notNull(handler, "Dispatch handler must not be null");
    synchronized(this) {
      if (this.dispatchedPath == null) {
        this.dispatchHandlers.add(handler);
      }
      else {
        handler.run();
      }
    }
  }

  @Override
  public ServletRequest getRequest() {
    return this.request;
  }

  @Override
  @Nullable
  public ServletResponse getResponse() {
    return this.response;
  }

  @Override
  public boolean hasOriginalRequestAndResponse() {
    return (this.request instanceof MockHttpServletRequest && this.response instanceof MockHttpServletResponse);
  }

  @Override
  public void dispatch() {
    dispatch(this.request.getRequestURI());
  }

  @Override
  public void dispatch(String path) {
    dispatch(null, path);
  }

  @Override
  public void dispatch(@Nullable ServletContext context, String path) {
    synchronized(this) {
      this.dispatchedPath = path;
      this.dispatchHandlers.forEach(Runnable::run);
    }
  }

  @Nullable
  public String getDispatchedPath() {
    return this.dispatchedPath;
  }

  @Override
  public void complete() {
    MockHttpServletRequest mockRequest = WebUtils.getNativeRequest(this.request, MockHttpServletRequest.class);
    if (mockRequest != null) {
      mockRequest.setAsyncStarted(false);
    }
    for (AsyncListener listener : this.listeners) {
      try {
        listener.onComplete(new AsyncEvent(this, this.request, this.response));
      }
      catch (IOException ex) {
        throw new IllegalStateException("AsyncListener failure", ex);
      }
    }
  }

  @Override
  public void start(Runnable runnable) {
    runnable.run();
  }

  @Override
  public void addListener(AsyncListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void addListener(AsyncListener listener, ServletRequest request, ServletResponse response) {
    this.listeners.add(listener);
  }

  public List<AsyncListener> getListeners() {
    return this.listeners;
  }

  @Override
  public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
    return BeanUtils.instantiateClass(clazz);
  }

  /**
   * By default this is set to 10000 (10 seconds) even though the Servlet API
   * specifies a default async request timeout of 30 seconds. Keep in mind the
   * timeout could further be impacted by global configuration through the MVC
   * Java config or the XML namespace, as well as be overridden per request on
   * {@link cn.taketoday.web.context.request.async.DeferredResult DeferredResult}
   * or on
   * {@link cn.taketoday.web.servlet.mvc.method.annotation.SseEmitter SseEmitter}.
   *
   * @param timeout the timeout value to use.
   * @see AsyncContext#setTimeout(long)
   */
  @Override
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public long getTimeout() {
    return this.timeout;
  }

}
