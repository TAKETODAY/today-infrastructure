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

package cn.taketoday.mock.api;

/**
 * Event that gets fired when the asynchronous operation initiated on a ServletRequest (via a call to
 * {@link MockRequest#startAsync} or {@link MockRequest#startAsync(MockRequest, MockResponse)}) has
 * completed, timed out, or produced an error.
 */
public class AsyncEvent {

  private AsyncContext context;
  private MockRequest request;
  private MockResponse response;
  private Throwable throwable;

  /**
   * Constructs an AsyncEvent from the given AsyncContext.
   *
   * @param context the AsyncContex to be delivered with this AsyncEvent
   */
  public AsyncEvent(AsyncContext context) {
    this(context, context.getRequest(), context.getResponse(), null);
  }

  /**
   * Constructs an AsyncEvent from the given AsyncContext, ServletRequest, and ServletResponse.
   *
   * @param context the AsyncContex to be delivered with this AsyncEvent
   * @param request the ServletRequest to be delivered with this AsyncEvent
   * @param response the ServletResponse to be delivered with this AsyncEvent
   */
  public AsyncEvent(AsyncContext context, MockRequest request, MockResponse response) {
    this(context, request, response, null);
  }

  /**
   * Constructs an AsyncEvent from the given AsyncContext and Throwable.
   *
   * @param context the AsyncContex to be delivered with this AsyncEvent
   * @param throwable the Throwable to be delivered with this AsyncEvent
   */
  public AsyncEvent(AsyncContext context, Throwable throwable) {
    this(context, context.getRequest(), context.getResponse(), throwable);
  }

  /**
   * Constructs an AsyncEvent from the given AsyncContext, ServletRequest, ServletResponse, and Throwable.
   *
   * @param context the AsyncContex to be delivered with this AsyncEvent
   * @param request the ServletRequest to be delivered with this AsyncEvent
   * @param response the ServletResponse to be delivered with this AsyncEvent
   * @param throwable the Throwable to be delivered with this AsyncEvent
   */
  public AsyncEvent(AsyncContext context, MockRequest request, MockResponse response, Throwable throwable) {
    this.context = context;
    this.request = request;
    this.response = response;
    this.throwable = throwable;
  }

  /**
   * Gets the AsyncContext from this AsyncEvent.
   *
   * @return the AsyncContext that was used to initialize this AsyncEvent
   */
  public AsyncContext getAsyncContext() {
    return context;
  }

  /**
   * Gets the ServletRequest from this AsyncEvent.
   *
   * <p>
   * If the AsyncListener to which this AsyncEvent is being delivered was added using
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the returned ServletRequest will be
   * the same as the one supplied to the above method. If the AsyncListener was added via
   * {@link AsyncContext#addListener(AsyncListener)}, this method must return null.
   *
   * @return the ServletRequest that was used to initialize this AsyncEvent, or null if this AsyncEvent was initialized
   * without any ServletRequest
   */
  public MockRequest getSuppliedRequest() {
    return request;
  }

  /**
   * Gets the ServletResponse from this AsyncEvent.
   *
   * <p>
   * If the AsyncListener to which this AsyncEvent is being delivered was added using
   * {@link AsyncContext#addListener(AsyncListener, MockRequest, MockResponse)}, the returned ServletResponse will
   * be the same as the one supplied to the above method. If the AsyncListener was added via
   * {@link AsyncContext#addListener(AsyncListener)}, this method must return null.
   *
   * @return the ServletResponse that was used to initialize this AsyncEvent, or null if this AsyncEvent was initialized
   * without any ServletResponse
   */
  public MockResponse getSuppliedResponse() {
    return response;
  }

  /**
   * Gets the Throwable from this AsyncEvent.
   *
   * @return the Throwable that was used to initialize this AsyncEvent, or null if this AsyncEvent was initialized without
   * any Throwable
   */
  public Throwable getThrowable() {
    return throwable;
  }

}
