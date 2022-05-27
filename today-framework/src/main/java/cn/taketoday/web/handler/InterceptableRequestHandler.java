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

package cn.taketoday.web.handler;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerInterceptorsProvider;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2019-12-25 16:19
 */
public abstract class InterceptableRequestHandler
        extends HandlerInterceptorHolder implements HttpRequestHandler, HandlerInterceptorsProvider {

  public InterceptableRequestHandler() { }

  public InterceptableRequestHandler(HandlerInterceptor... interceptors) {
    setInterceptors(interceptors);
  }

  /**
   * perform {@link HandlerInterceptor} on this handler
   *
   * @param request Current request context
   * @return handler's result
   * @throws Throwable any exception occurred in this request context
   */
  @Nullable
  @Override
  public Object handleRequest(final RequestContext request) throws Throwable {
    HandlerInterceptor[] interceptors = this.interceptors.get();
    if (interceptors == null) {
      return handleInternal(request);
    }
    // @since 4.0
    return new Chain(interceptors, this).proceed(request);
  }

  /**
   * perform this handler' behavior internal
   */
  @Nullable
  protected abstract Object handleInternal(final RequestContext context)
          throws Throwable;

  private final class Chain extends InterceptorChain {

    private Chain(HandlerInterceptor[] interceptors, Object handler) {
      super(interceptors, handler);
    }

    @Override
    protected Object invokeHandler(RequestContext context, Object handler) throws Throwable {
      return handleInternal(context);
    }
  }

}
