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

import java.io.IOException;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Process Handler not found
 *
 * @author TODAY 2019-12-20 19:15
 */
public class NotFoundHandler {

  private static final Logger log = LoggerFactory.getLogger(NotFoundHandler.class);

  /**
   * Process not found
   */
  public Object handleNotFound(RequestContext context) throws IOException {
    logNotFound(context);

    context.sendError(HttpStatus.NOT_FOUND.value());
    return HandlerAdapter.NONE_RETURN_VALUE;
  }

  protected void logNotFound(RequestContext context) {
    if (log.isDebugEnabled()) {
      log.debug("No mapping for {} {}", context.getMethodValue(), context.getRequestPath());
    }
  }

}
