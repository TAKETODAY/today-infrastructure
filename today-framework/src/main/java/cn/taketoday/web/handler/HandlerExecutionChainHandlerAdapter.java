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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 16:26
 */
public class HandlerExecutionChainHandlerAdapter implements HandlerAdapter {
  private final HandlerAdapter handlerAdapter;

  public HandlerExecutionChainHandlerAdapter(ApplicationContext context) {
    this(HandlerAdapter.find(context));
  }

  public HandlerExecutionChainHandlerAdapter(HandlerAdapter handlerAdapter) {
    this.handlerAdapter = handlerAdapter;
  }

  @Override
  public boolean supports(Object handler) {
    return handler instanceof HandlerExecutionChain;
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    HandlerExecutionChain chain = (HandlerExecutionChain) handler;

    Object targetHandler = chain.getHandler();
    HandlerInterceptor[] interceptors = chain.getInterceptors();

    if (interceptors == null) {
      return handlerAdapter.handle(context, targetHandler);
    }

    return new InterceptableRequestHandler(interceptors) {

      @Override
      protected Object handleInternal(RequestContext context) throws Throwable {
        return handlerAdapter.handle(context, targetHandler);
      }

    }.handleRequest(context);
  }

}
