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
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;

/**
 * @author TODAY 2019-07-14 01:14
 */
public class ModelAndViewReturnValueHandler
        extends HandlerMethodReturnValueHandler implements ReturnValueHandler {

  private final SelectableReturnValueHandler returnValueHandlers;

  public ModelAndViewReturnValueHandler(List<ReturnValueHandler> returnValueHandlers) {
    this.returnValueHandlers = new SelectableReturnValueHandler(returnValueHandlers);
  }

  public ModelAndViewReturnValueHandler(SelectableReturnValueHandler returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    return handlerMethod.isReturnTypeAssignableTo(ModelAndView.class);
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue instanceof ModelAndView;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws IOException {
    if (returnValue instanceof ModelAndView) {
      handleModelAndView(context, handler, (ModelAndView) returnValue);
    }
  }

  /**
   * Resolve {@link ModelAndView} return type
   *
   * @since 2.3.3
   */
  public final void handleModelAndView(
          RequestContext context, @Nullable Object handler, @Nullable ModelAndView modelAndView) throws IOException {
    if (modelAndView != null && modelAndView.hasView()) {
      returnValueHandlers.handleReturnValue(context, handler, modelAndView.getView());
    }
  }

}
