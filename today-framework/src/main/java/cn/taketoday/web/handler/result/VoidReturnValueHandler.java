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
package cn.taketoday.web.handler.result;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.view.ModelAndView;

/**
 * for {@link Void} or void type
 *
 * @author TODAY 2019-07-14 00:53
 */
public class VoidReturnValueHandler implements HandlerMethodReturnValueHandler {
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

  /**
   * mainly for ModelAndView
   *
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   */
  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {

    BindingContext bindingContext = context.getBindingContext();
    if (bindingContext != null && bindingContext.hasModelAndView()) {
      ModelAndView modelAndView = bindingContext.getModelAndView();
      // user constructed a ModelAndView hold in context
      returnValueHandler.handleModelAndView(context, null, modelAndView);
    }
  }

}
