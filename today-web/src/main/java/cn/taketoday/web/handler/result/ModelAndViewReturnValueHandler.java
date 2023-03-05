/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewReturnValueHandler;

/**
 * Handles return values of type {@link ModelAndView}
 *
 * @author TODAY 2019-07-14 01:14
 */
public class ModelAndViewReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final ViewReturnValueHandler delegate;

  public ModelAndViewReturnValueHandler(ViewReturnValueHandler delegate) {
    Assert.notNull(delegate, "ViewReturnValueHandler is required");
    this.delegate = delegate;
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
          RequestContext context, Object handler, Object returnValue) throws Exception {
    if (returnValue instanceof ModelAndView modelAndView) {
      handleModelAndView(context, modelAndView);
    }
  }

  /**
   * Resolve {@link ModelAndView} return type
   *
   * @since 2.3.3
   */
  public final void handleModelAndView(
          RequestContext context, @Nullable ModelAndView mv) throws Exception {
    if (mv != null) {
      View view = mv.getView();
      String viewName = mv.getViewName();

      BindingContext bindingContext = context.getBindingContext();
      if (bindingContext != null) {
        bindingContext.addAllAttributes(mv.getModel());
      }

      if (mv.getStatus() != null) {
        context.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, mv.getStatus());
        context.setStatus(mv.getStatus().value());
      }

      if (viewName != null) {
        delegate.renderView(context, viewName);
      }
      else if (view != null) {
        delegate.renderView(context, view);
      }
    }
  }

}
