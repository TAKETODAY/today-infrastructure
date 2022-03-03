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

import java.util.List;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2020/12/23 21:53
 */
public class CompositeHandlerExceptionHandler
        extends OrderedSupport implements HandlerExceptionHandler {

  private List<HandlerExceptionHandler> handlers;

  public CompositeHandlerExceptionHandler() { }

  public CompositeHandlerExceptionHandler(final List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Set the list of exception resolvers to delegate to.
   */
  public void setExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Return the list of exception resolvers to delegate to.
   */
  public List<HandlerExceptionHandler> getExceptionHandlers() {
    return this.handlers;
  }

  @Override
  public Object handleException(
          final RequestContext context, final Throwable exception, final Object handler) throws Exception {
    final List<HandlerExceptionHandler> handlers = getExceptionHandlers();
    if (handlers != null) {
      for (final HandlerExceptionHandler exceptionHandler : handlers) {
        final Object view = exceptionHandler.handleException(context, exception, handler);
        if (view != null) {
          return view;
        }
      }
    }
    return HandlerAdapter.NONE_RETURN_VALUE;
  }
}
