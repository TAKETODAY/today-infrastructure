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

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.HandlerAdapter;
import infra.web.HandlerAdapterNotFoundException;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;

/**
 * composite HandlerAdapter
 * <p>
 * default supports HttpRequestHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/10 15:18
 */
public class HandlerAdapters implements HandlerAdapter {
  private final HandlerAdapter[] handlerAdapters;

  public HandlerAdapters(HandlerAdapter[] handlerAdapters) {
    Assert.notNull(handlerAdapters, "HandlerAdapters is required");
    this.handlerAdapters = handlerAdapters;
  }

  @Override
  public boolean supports(Object handler) {
    return selectAdapter(handler) != null;
  }

  @Nullable
  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    if (handler instanceof HttpRequestHandler httpRequestHandler) {
      return httpRequestHandler.handleRequest(context);
    }

    for (HandlerAdapter handlerAdapter : handlerAdapters) {
      if (handlerAdapter.supports(handler)) {
        return handlerAdapter.handle(context, handler);
      }
    }
    throw new HandlerAdapterNotFoundException(handler);
  }

  public @Nullable HandlerAdapter selectAdapter(Object handler) {
    for (HandlerAdapter handlerAdapter : handlerAdapters) {
      if (handlerAdapter.supports(handler)) {
        return handlerAdapter;
      }
    }
    return null;
  }

}
