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

import java.io.IOException;

import cn.taketoday.core.NonNull;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;

/**
 * for HandlerMethod return-value
 *
 * @author TODAY 2019-12-13 13:52
 * @see HandlerMethod#handle(RequestContext, Object)
 * @see HandlerMethod#handleRequest(RequestContext)
 */
public abstract class HandlerMethodReturnValueHandler implements ReturnValueHandler {

  @Override
  public final boolean supportsHandler(final Object handler) {
    return handler instanceof HandlerMethod && supportsHandlerMethod((HandlerMethod) handler);
  }

  /**
   * match function for {@link HandlerMethod}
   *
   * @see HandlerMethod
   */
  protected abstract boolean supportsHandlerMethod(HandlerMethod handler);

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws Throwable {
    if (returnValue != null) {
      handleInternal(context, (HandlerMethod) handler, returnValue);
    }
    else {
      handleNullValue(context, handler);
    }
  }

  protected void handleNullValue(RequestContext context, Object handler) throws IOException { }

  protected void handleInternal(
          RequestContext context, HandlerMethod handler, @NonNull Object returnValue) throws Throwable { }
}
