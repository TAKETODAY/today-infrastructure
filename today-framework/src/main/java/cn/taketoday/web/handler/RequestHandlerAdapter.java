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

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;

/**
 * Adapt RequestHandler to handle request
 *
 * @author TODAY 2019-12-23 21:50
 */
public class RequestHandlerAdapter extends OrderedSupport implements HandlerAdapter {

  public RequestHandlerAdapter() { }

  public RequestHandlerAdapter(int order) {
    setOrder(order);
  }

  @Override
  public boolean supports(Object handler) {
    return handler instanceof HttpRequestHandler;
  }

  @Override
  public Object handle(final RequestContext context, final Object handler) throws Throwable {
    return ((HttpRequestHandler) handler).handleRequest(context);
  }

}
