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

package cn.taketoday.mock.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.web.async.DeferredResult;
import cn.taketoday.web.mock.MockUtils;
import cn.taketoday.mock.api.AsyncContext;
import cn.taketoday.mock.api.AsyncEvent;
import cn.taketoday.mock.api.AsyncListener;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;

/**
 * Mock implementation of the {@link AsyncContext} interface.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockAsyncContext implements AsyncContext {

  private final HttpMockRequest request;

  @Nullable
  private final HttpMockResponse response;

  private final List<AsyncListener> listeners = new ArrayList<>();

  @Nullable
  private String dispatchedPath;

  private long timeout = 10 * 1000L;

  private final List<Runnable> dispatchHandlers = new ArrayList<>();

  public MockAsyncContext(MockRequest request, @Nullable MockResponse response) {
    this.request = (HttpMockRequest) request;
    this.response = (HttpMockResponse) response;
  }

  public void addDispatchHandler(Runnable handler) {
    Assert.notNull(handler, "Dispatch handler is required");
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
  public MockRequest getRequest() {
    return this.request;
  }

  @Override
  @Nullable
  public MockResponse getResponse() {
    return this.response;
  }

  @Override
  public boolean hasOriginalRequestAndResponse() {
    return (this.request instanceof HttpMockRequestImpl && this.response instanceof MockHttpResponseImpl);
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
  public void dispatch(@Nullable MockContext context, String path) {
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
    HttpMockRequestImpl mockRequest = MockUtils.getNativeRequest(this.request, HttpMockRequestImpl.class);
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
  public void addListener(AsyncListener listener, MockRequest request, MockResponse response) {
    this.listeners.add(listener);
  }

  public List<AsyncListener> getListeners() {
    return this.listeners;
  }

  @Override
  public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
    return BeanUtils.newInstance(clazz);
  }

  /**
   * By default this is set to 10000 (10 seconds) even though the Servlet API
   * specifies a default async request timeout of 30 seconds. Keep in mind the
   * timeout could further be impacted by global configuration through the MVC
   * Java config or the XML namespace, as well as be overridden per request on
   * {@link DeferredResult DeferredResult}
   * or on
   * {@link cn.taketoday.web.handler.method.SseEmitter SseEmitter}.
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
