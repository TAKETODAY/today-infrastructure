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

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.InterceptorChain;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/18 10:23
 */
public class InterceptableHandlerAdapterDecorator extends HandlerInterceptorHolder implements HandlerAdapter {

  private final HandlerAdapter handlerAdapter;

  public InterceptableHandlerAdapterDecorator(HandlerAdapter handlerAdapter) {
    Assert.notNull(handlerAdapter, "handlerAdapter is required");
    this.handlerAdapter = handlerAdapter;
  }

  @Override
  public boolean supports(Object handler) {
    return handlerAdapter.supports(handler);
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    HandlerInterceptor[] interceptors = this.interceptors.get();
    if (ObjectUtils.isNotEmpty(interceptors)) {
      return new Chain(interceptors, handler).proceed(context);
    }
    return handlerAdapter.handle(context, handler);
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
