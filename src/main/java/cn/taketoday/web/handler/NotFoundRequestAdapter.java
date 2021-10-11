/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.IOException;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Process Handler not found ,handler is null
 *
 * @author TODAY 2019-12-20 19:15
 */
public class NotFoundRequestAdapter extends AbstractHandlerAdapter {
  private static final Logger log = LoggerFactory.getLogger(NotFoundRequestAdapter.class);

  public NotFoundRequestAdapter() { }

  public NotFoundRequestAdapter(int order) {
    setOrder(order);
  }

  @Override
  public final boolean supports(Object handler) {
    return handler == null;
  }

  @Override
  public Object handle(final RequestContext context, final Object handler) throws Throwable {
    logNotFound(context);
    return handleNotFound(context);
  }

  protected Object handleNotFound(final RequestContext context) throws IOException {
    context.sendError(404);
    return NONE_RETURN_VALUE;
  }

  protected void logNotFound(RequestContext context) {
    if (log.isDebugEnabled()) {
      log.debug("NOT FOUND -> [{} {}]", context.getMethod(), context.getRequestPath());
    }
  }

}
