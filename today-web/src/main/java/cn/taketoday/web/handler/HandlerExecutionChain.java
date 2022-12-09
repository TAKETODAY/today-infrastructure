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

package cn.taketoday.web.handler;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
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
public class HandlerExecutionChain
        extends InterceptableRequestHandler implements HandlerWrapper, HandlerAdapterAware {

  private final Object handler;

  private HandlerAdapter handlerAdapter;

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   */
  public HandlerExecutionChain(Object handler) {
    this(handler, (HandlerInterceptor[]) null);
  }

  /**
   * Create a new HandlerExecutionChain.
   *
   * @param handler the handler object to execute
   * @param interceptors the array of interceptors to apply
   * (in the given order) before the handler itself executes
   */
  public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {
    if (handler instanceof HandlerExecutionChain originalChain) {
      this.handler = originalChain.getHandler();
      addInterceptors(originalChain.getInterceptors());
    }
    else {
      this.handler = handler;
    }
    addInterceptors(interceptors);
  }

  /**
   * Return the handler object to execute.
   */
  @Override
  public Object getHandler() {
    return this.handler;
  }

  /**
   * Add the given interceptor to the end of this chain.
   */
  public void addInterceptor(HandlerInterceptor interceptor) {
    interceptors.add(interceptor);
  }

  /**
   * Add the given interceptor at the specified index of this chain.
   */
  public void addInterceptor(int index, HandlerInterceptor interceptor) {
    interceptors.add(index, interceptor);
  }

  /**
   * Delegates to the handler's {@code toString()} implementation.
   */
  @Override
  public String toString() {
    return "HandlerExecutionChain with [" + getHandler() + "] and " + interceptorSize() + " interceptors";
  }

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context) throws Throwable {
    return handlerAdapter.handle(context, handler);
  }

  @Override
  public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
    this.handlerAdapter = handlerAdapter;
  }

}
