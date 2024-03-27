/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.result;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.ViewReturnValueHandler;

/**
 * for {@link Void} or void type
 *
 * @author TODAY 2019-07-14 00:53
 */
public class VoidReturnValueHandler implements SmartReturnValueHandler {
  private final ViewReturnValueHandler delegate;

  public VoidReturnValueHandler(ViewReturnValueHandler delegate) {
    Assert.notNull(delegate, "ViewReturnValueHandler is required");
    this.delegate = delegate;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue == null;
  }

  @Override
  public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    if (returnValue == null) {
      HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
      return handlerMethod == null || handlerMethod.isReturn(void.class) || handlerMethod.isReturn(Void.class);
    }
    return false;
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
  public void handleReturnValue(RequestContext context,
          @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    BindingContext binding = context.getBinding();
    if (binding != null && binding.hasModelAndView()) {
      ModelAndView modelAndView = binding.getModelAndView();
      // user constructed a ModelAndView hold in context
      delegate.renderView(context, modelAndView);
    }
  }

}
