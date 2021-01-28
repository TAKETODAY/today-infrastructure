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
package cn.taketoday.web.view;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY
 * @date 2020/12/23 20:12
 */
public class HttpStatusResultHandler
        extends HandlerMethodResultHandler implements RuntimeResultHandler {

  @Override
  protected boolean supports(final HandlerMethod handler) {
    return handler.is(HttpStatus.class);
  }

  @Override
  public boolean supportsResult(final Object result) {
    return result instanceof HttpStatus;
  }

  @Override
  public void handleResult(final RequestContext context,
                           final Object handler, final Object result) throws Throwable {
    if (result instanceof HttpStatus) {
      context.status((HttpStatus) result);
    }
  }

}
