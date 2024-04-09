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

package cn.taketoday.web.handler;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;

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

  @Nullable
  private final HandlerInterceptor[] interceptors;

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
  public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor[] interceptors) {
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
   * Create a new HandlerExecutionChain.
   *
   * @param interceptors the array of interceptors to apply
   * (in the given order) before the handler itself executes
   */
  public HandlerExecutionChain withInterceptors(@Nullable HandlerInterceptor[] interceptors) {
    return new HandlerExecutionChain(handler, interceptors);
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
    HandlerInterceptor[] interceptors = this.interceptors;
    if (interceptors == null) {
      return handlerAdapter.handle(request, handler);
    }
    return new Chain(interceptors, handler).proceed(request);
  }

  @Nullable
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  private final class Chain extends InterceptorChain {

    private Chain(HandlerInterceptor[] interceptors, Object handler) {
      super(interceptors, handler);
    }

    @Override
    protected Object invokeHandler(RequestContext context, Object handler) throws Throwable {
      return handlerAdapter.handle(context, handler);
    }
  }

}
