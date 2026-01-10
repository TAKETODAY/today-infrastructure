/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.BindingContext;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;
import infra.web.view.ModelAndView;
import infra.web.view.ViewReturnValueHandler;

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
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue == null;
  }

  @Override
  public boolean supportsHandler(@Nullable Object handler, @Nullable Object returnValue) {
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
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    BindingContext binding = context.getBinding();
    if (binding != null && binding.hasModelAndView()) {
      ModelAndView modelAndView = binding.getModelAndView();
      // user constructed a ModelAndView hold in context
      delegate.renderView(context, modelAndView);
    }
  }

}
