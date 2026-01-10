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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import infra.web.HandlerAdapter;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.HttpRequestHandler;
import infra.web.InterceptorChain;
import infra.web.RequestContext;

/**
 * Handler execution chain, consisting of handler object and any handler interceptors.
 * Returned by HandlerMapping's {@link HandlerMapping#getHandler} method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerInterceptor
 * @since 4.0 2022/5/22 22:42
 */
public class HandlerExecutionChain implements HandlerWrapper, HandlerAdapterAware, HttpRequestHandler {

  private final Object handler;

  private HandlerAdapter handlerAdapter;

  private final HandlerInterceptor @Nullable [] interceptors;

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   */
  public HandlerExecutionChain(Object handler) {
    this(handler, null);
  }

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   * @param interceptors the array of interceptors to apply
   * (in the given order) before the handler itself executes
   */
  @SuppressWarnings("NullAway")
  public HandlerExecutionChain(Object handler, HandlerInterceptor @Nullable [] interceptors) {
    this.handler = handler;
    this.interceptors = interceptors;
  }

  /**
   * Return the handler object to execute.
   */
  @Override
  public Object getRawHandler() {
    return this.handler;
  }

  @Override
  public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
    this.handlerAdapter = handlerAdapter;
  }

  /**
   * Delegates to the handler's {@code toString()} implementation.
   */
  @Override
  public String toString() {
    return "HandlerExecutionChain with [%s] and %d interceptors"
            .formatted(handler, interceptors != null ? interceptors.length : 0);
  }

  @Nullable
  @Override
  public Object handleRequest(RequestContext request) throws Throwable {
    var interceptors = this.interceptors;
    if (interceptors == null) {
      return handlerAdapter.handle(request, handler);
    }
    return new Chain(interceptors, handler).proceed(request);
  }

  public HandlerInterceptor @Nullable [] getInterceptors() {
    return interceptors;
  }

  private final class Chain extends InterceptorChain {

    private Chain(HandlerInterceptor[] interceptors, Object handler) {
      super(interceptors, handler);
    }

    @Nullable
    @Override
    protected Object invokeHandler(RequestContext context, Object handler) throws Throwable {
      return handlerAdapter.handle(context, handler);
    }
  }

}
