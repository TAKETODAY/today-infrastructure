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

package cn.taketoday.web;

/**
 * HandlerInterceptor execution chain
 *
 * @author TODAY 2021/8/8 14:57
 * @since 4.0
 */
public abstract class InterceptorChain {

  private int currentIndex = 0;
  private final Object handler;
  private final int interceptorLength;
  private final HandlerInterceptor[] interceptors;

  protected InterceptorChain(HandlerInterceptor[] interceptors, Object handler) {
    this.interceptorLength = interceptors.length;
    this.interceptors = interceptors;
    this.handler = handler;
  }

  /**
   * Execute next interceptor
   *
   * @param context current request context
   * @return interceptor or handler result, this will handle by {@link cn.taketoday.web.ReturnValueHandler}
   * @throws Throwable if interceptor throw exception
   * @see cn.taketoday.web.ReturnValueHandler
   */
  public final Object proceed(RequestContext context) throws Throwable {
    if (currentIndex < interceptorLength) {
      return interceptors[currentIndex++].intercept(context, this);
    }
    return invokeHandler(context, handler);
  }

  /**
   * process target handler
   *
   * @param context current context
   * @param handler this context request handler
   * @return handle result
   */
  protected abstract Object invokeHandler(RequestContext context, Object handler) throws Throwable;

  /**
   * Get interceptors
   */
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  /**
   * Get current interceptor's index
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * target handler
   */
  public Object getHandler() {
    return handler;
  }

}
