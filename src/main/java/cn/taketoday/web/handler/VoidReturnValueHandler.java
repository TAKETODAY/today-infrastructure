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

import java.io.IOException;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * for {@link Void} or void type
 *
 * @author TODAY 2019-07-14 00:53
 */
public class VoidReturnValueHandler
        extends HandlerMethodReturnValueHandler implements ReturnValueHandler {
  private final ModelAndViewReturnValueHandler returnValueHandler;

  public VoidReturnValueHandler(ModelAndViewReturnValueHandler returnValueHandler) {
    Assert.notNull(returnValueHandler, "ModelAndViewReturnValueHandler must not be null");
    this.returnValueHandler = returnValueHandler;
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    return handlerMethod.isReturn(void.class)
            || handlerMethod.isReturn(Void.class);
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue == null;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    if (context.hasModelAndView()) {
      // user constructed a ModelAndView hold in context
      returnValueHandler.handleModelAndView(context, null, context.modelAndView());
    }
  }

}
