/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler;

import java.util.List;

import infra.core.OrderedSupport;
import infra.lang.Nullable;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;

/**
 * @author TODAY 2020/12/23 21:53
 */
public class CompositeHandlerExceptionHandler extends OrderedSupport implements HandlerExceptionHandler {

  @Nullable
  private List<HandlerExceptionHandler> handlers;

  public CompositeHandlerExceptionHandler(final List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Set the list of exception resolvers to delegate to.
   */
  public void setExceptionHandlers(@Nullable List<HandlerExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  /**
   * Return the list of exception resolvers to delegate to.
   */
  @Nullable
  public List<HandlerExceptionHandler> getExceptionHandlers() {
    return this.handlers;
  }

  @Nullable
  @Override
  public Object handleException(final RequestContext context, final Throwable exception, @Nullable final Object handler) throws Exception {
    var handlers = getExceptionHandlers();
    if (handlers != null) {
      for (var exceptionHandler : handlers) {
        Object view = exceptionHandler.handleException(context, exception, handler);
        if (view != null) {
          return view;
        }
      }
    }
    return null;
  }
}
