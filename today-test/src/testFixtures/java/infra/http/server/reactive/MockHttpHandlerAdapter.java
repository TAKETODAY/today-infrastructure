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

package infra.http.server.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpLogging;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.mock.api.AsyncContext;
import infra.mock.api.AsyncEvent;
import infra.mock.api.AsyncListener;
import infra.mock.api.DispatcherType;
import infra.mock.api.MockApi;
import infra.mock.api.MockConfig;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMock;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;

/**
 * Adapt {@link HttpHandler} to an {@link HttpMock} using Servlet Async support
 * and Servlet 3.1 non-blocking I/O.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockHttpHandlerAdapter implements MockApi {
  private static final Logger logger = HttpLogging.forLogName(MockHttpHandlerAdapter.class);

  private static final int DEFAULT_BUFFER_SIZE = 8192;
  private static final String WRITE_ERROR_ATTRIBUTE_NAME = MockHttpHandlerAdapter.class.getName() + ".ERROR";

  private final HttpHandler httpHandler;
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;

  @Nullable
  private String mockPath;

  public MockHttpHandlerAdapter(HttpHandler httpHandler) {
    Assert.notNull(httpHandler, "HttpHandler is required");
    this.httpHandler = httpHandler;
  }

  /**
   * Set the size of the input buffer used for reading in bytes.
   * <p>By default this is set to 8192.
   */
  public void setBufferSize(int bufferSize) {
    Assert.isTrue(bufferSize > 0, "Buffer size must be larger than zero");
    this.bufferSize = bufferSize;
  }

  /**
   * Return the configured input buffer size.
   */
  public int getBufferSize() {
    return this.bufferSize;
  }

  /**
   * Return the Servlet path under which the Servlet is deployed by checking
   * the Servlet registration from {@link #init(MockConfig)}.
   *
   * @return the path, or an empty string if the Servlet is deployed without
   * a prefix (i.e. "/" or "/*"), or {@code null} if this method is invoked
   * before the {@link #init(MockConfig)} Servlet container callback.
   */
  @Nullable
  public String getServletPath() {
    return this.mockPath;
  }

  public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
    Assert.notNull(dataBufferFactory, "DataBufferFactory is required");
    this.dataBufferFactory = dataBufferFactory;
  }

  public DataBufferFactory getDataBufferFactory() {
    return this.dataBufferFactory;
  }

  // Servlet methods...

  @Override
  public void init(MockConfig config) {
    this.mockPath = "";
  }

  @Override
  public void service(MockRequest request, MockResponse response) throws MockException, IOException {
    // Check for existing error attribute first
    if (DispatcherType.ASYNC == request.getDispatcherType()) {
      Throwable ex = (Throwable) request.getAttribute(WRITE_ERROR_ATTRIBUTE_NAME);
      throw new MockException("Failed to create response content", ex);
    }

    // Start async before Read/WriteListener registration
    AsyncContext asyncContext = request.startAsync();
    asyncContext.setTimeout(-1);

    MockServerHttpRequest httpRequest;
    AsyncListener requestListener;
    String logPrefix;
    try {
      httpRequest = createRequest(((HttpMockRequest) request), asyncContext);
      requestListener = httpRequest.getAsyncListener();
      logPrefix = httpRequest.getLogPrefix();
    }
    catch (URISyntaxException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Failed to get request  URL: {}", ex.getMessage());
      }
      ((HttpMockResponse) response).setStatus(400);
      asyncContext.complete();
      return;
    }

    ServerHttpResponse httpResponse = createResponse(((HttpMockResponse) response), asyncContext, httpRequest);
    AsyncListener responseListener = ((MockServerHttpResponse) httpResponse).getAsyncListener();
    if (httpRequest.getMethod() == HttpMethod.HEAD) {
      httpResponse = new HttpHeadResponseDecorator(httpResponse);
    }

    AtomicBoolean completionFlag = new AtomicBoolean();
    HandlerResultSubscriber subscriber = new HandlerResultSubscriber(asyncContext, completionFlag, logPrefix);

    asyncContext.addListener(new HttpHandlerAsyncListener(
            requestListener, responseListener, subscriber, completionFlag, logPrefix));

    this.httpHandler.handle(httpRequest, httpResponse).subscribe(subscriber);
  }

  protected MockServerHttpRequest createRequest(HttpMockRequest request, AsyncContext context)
          throws IOException, URISyntaxException {

    Assert.notNull(this.mockPath, "Servlet path is not initialized");
    return new MockServerHttpRequest(
            request, context, this.mockPath, getDataBufferFactory(), getBufferSize());
  }

  protected MockServerHttpResponse createResponse(
          HttpMockResponse response,
          AsyncContext context, MockServerHttpRequest request) throws IOException {

    return new MockServerHttpResponse(response, context, getDataBufferFactory(), getBufferSize(), request);
  }

  @Override
  public String getMockInfo() {
    return "";
  }

  @Override
  @Nullable
  public MockConfig getMockConfig() {
    return null;
  }

  @Override
  public void destroy() { }

  private static void runIfAsyncNotComplete(
          AsyncContext asyncContext, AtomicBoolean isCompleted, Runnable task) {
    try {
      if (asyncContext.getRequest().isAsyncStarted() && isCompleted.compareAndSet(false, true)) {
        task.run();
      }
    }
    catch (IllegalStateException ex) {
      // Ignore: AsyncContext recycled and should not be used
      // e.g. TIMEOUT_LISTENER (above) may have completed the AsyncContext
    }
  }

  /**
   * AsyncListener to complete the {@link AsyncContext} in case of error or
   * timeout notifications from the container
   * <p>Additional {@link AsyncListener}s are registered in
   * {@link MockServerHttpRequest} to signal onError/onComplete to the
   * request body Subscriber, and in {@link MockServerHttpResponse} to
   * cancel the write Publisher and signal onError/onComplete downstream to
   * the writing result Subscriber.
   */
  private static class HttpHandlerAsyncListener implements AsyncListener {

    private final String logPrefix;
    private final AsyncListener requestAsyncListener;
    private final AsyncListener responseAsyncListener;
    // We cannot have AsyncListener and HandlerResultSubscriber until WildFly 12+:
    // https://issues.jboss.org/browse/WFLY-8515
    private final Runnable handlerDisposeTask;
    private final AtomicBoolean completionFlag;

    public HttpHandlerAsyncListener(
            AsyncListener requestAsyncListener, AsyncListener responseAsyncListener,
            Runnable handlerDisposeTask, AtomicBoolean completionFlag, String logPrefix) {

      this.requestAsyncListener = requestAsyncListener;
      this.responseAsyncListener = responseAsyncListener;
      this.handlerDisposeTask = handlerDisposeTask;
      this.completionFlag = completionFlag;
      this.logPrefix = logPrefix;
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      // Should never happen since we call asyncContext.setTimeout(-1)
      if (logger.isDebugEnabled()) {
        logger.debug("{}AsyncEvent onTimeout", this.logPrefix);
      }
      delegateTimeout(this.requestAsyncListener, event);
      delegateTimeout(this.responseAsyncListener, event);
      handleTimeoutOrError(event);
    }

    @Override
    public void onError(AsyncEvent event) {
      Throwable ex = event.getThrowable();
      if (logger.isDebugEnabled()) {
        logger.debug("{}AsyncEvent onError: {}", this.logPrefix, (ex != null ? ex : "<no Throwable>"));
      }
      delegateError(this.requestAsyncListener, event);
      delegateError(this.responseAsyncListener, event);
      handleTimeoutOrError(event);
    }

    @Override
    public void onComplete(AsyncEvent event) {
      delegateComplete(this.requestAsyncListener, event);
      delegateComplete(this.responseAsyncListener, event);
    }

    private static void delegateTimeout(AsyncListener listener, AsyncEvent event) {
      try {
        listener.onTimeout(event);
      }
      catch (Exception ex) {
        // Ignore
      }
    }

    private static void delegateError(AsyncListener listener, AsyncEvent event) {
      try {
        listener.onError(event);
      }
      catch (Exception ex) {
        // Ignore
      }
    }

    private static void delegateComplete(AsyncListener listener, AsyncEvent event) {
      try {
        listener.onComplete(event);
      }
      catch (Exception ex) {
        // Ignore
      }
    }

    private void handleTimeoutOrError(AsyncEvent event) {
      AsyncContext context = event.getAsyncContext();
      runIfAsyncNotComplete(context, this.completionFlag, () -> {
        try {
          this.handlerDisposeTask.run();
        }
        finally {
          context.complete();
        }
      });
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
      // no-op
    }
  }

  private static class HandlerResultSubscriber implements Subscriber<Void>, Runnable {

    private final String logPrefix;
    private final AsyncContext asyncContext;
    private final AtomicBoolean completionFlag;

    @Nullable
    private volatile Subscription subscription;

    public HandlerResultSubscriber(
            AsyncContext asyncContext, AtomicBoolean completionFlag, String logPrefix) {
      this.logPrefix = logPrefix;
      this.asyncContext = asyncContext;
      this.completionFlag = completionFlag;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Void aVoid) {
      // no-op
    }

    @Override
    public void onError(Throwable ex) {
      if (logger.isTraceEnabled()) {
        logger.trace("{}onError: {}", this.logPrefix, ex.toString());
      }
      runIfAsyncNotComplete(this.asyncContext, this.completionFlag, () -> {
        if (this.asyncContext.getResponse().isCommitted()) {
          logger.trace("{}Dispatch to container, to raise the error on servlet thread", this.logPrefix);
          this.asyncContext.getRequest().setAttribute(WRITE_ERROR_ATTRIBUTE_NAME, ex);
          this.asyncContext.dispatch();
        }
        else {
          try {
            logger.trace("{}Setting ServletResponse status to 500 Server Error", this.logPrefix);
            this.asyncContext.getResponse().resetBuffer();
            ((HttpMockResponse) this.asyncContext.getResponse()).setStatus(500);
          }
          finally {
            this.asyncContext.complete();
          }
        }
      });
    }

    @Override
    public void onComplete() {
      if (logger.isTraceEnabled()) {
        logger.trace("{}onComplete", this.logPrefix);
      }
      runIfAsyncNotComplete(this.asyncContext, this.completionFlag, this.asyncContext::complete);
    }

    @Override
    public void run() {
      Subscription s = this.subscription;
      if (s != null) {
        s.cancel();
      }
    }
  }

}
