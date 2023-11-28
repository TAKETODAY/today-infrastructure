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
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerAdapterNotFoundException;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;

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

  @Nullable
  public HandlerAdapter selectAdapter(Object handler) {
    for (HandlerAdapter handlerAdapter : handlerAdapters) {
      if (handlerAdapter.supports(handler)) {
        return handlerAdapter;
      }
    }
    return null;
  }

}
