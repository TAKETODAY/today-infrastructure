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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import infra.lang.Assert;
import infra.web.async.DeferredResult;
import infra.web.handler.result.SseEmitter;
import infra.web.mock.api.AsyncContext;
import infra.web.mock.api.AsyncEvent;
import infra.web.mock.api.AsyncListener;
import infra.web.mock.api.MockContext;

/**
 * Mock implementation of the {@link AsyncContext} interface.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockAsyncContext implements AsyncContext {

  private final MockRequest request;

  @Nullable
  private final MockResponse response;

  private final List<AsyncListener> listeners = new ArrayList<>();

  @Nullable
  private String dispatchedPath;

  private long timeout = 10 * 1000L;

  private final List<Runnable> dispatchHandlers = new ArrayList<>();

  public MockAsyncContext(MockRequest request, @Nullable MockResponse response) {
    this.request = request;
    this.response = response;
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
    return (this.request instanceof MockRequest && this.response instanceof MockResponse);
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
    request.setAsyncStarted(false);

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

  /**
   * By default this is set to 10000 (10 seconds) even though the Mock API
   * specifies a default async request timeout of 30 seconds. Keep in mind the
   * timeout could further be impacted by global configuration through the MVC
   * Java config or the XML namespace, as well as be overridden per request on
   * {@link DeferredResult DeferredResult}
   * or on
   * {@link SseEmitter SseEmitter}.
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
