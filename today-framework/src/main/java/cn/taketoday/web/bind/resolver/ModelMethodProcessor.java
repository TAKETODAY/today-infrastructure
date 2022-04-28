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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.RedirectModel;

/**
 * Resolves {@link Model} arguments and handles {@link Model} return values.
 *
 * <p>A {@link Model} return type has a set purpose. Therefore this handler
 * should be configured ahead of handlers that support any return value type
 * annotated with {@code @ModelAttribute} or {@code @ResponseBody} to ensure
 * they don't take over.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RedirectModel
 * @see Model
 * @since 4.0 2022/4/27 16:51
 */
public class ModelMethodProcessor implements HandlerMethodReturnValueHandler, ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(Model.class) || resolvable.is(RedirectModel.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof Model;
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(Model.class);
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Model model) {
      BindingContext bindingContext = context.getBindingContext();
      if (returnValue instanceof RedirectModel redirectModel) {
        // RedirectModel is a special case:
        RedirectModel existRedirectModel = bindingContext.getRedirectModel();
        if (existRedirectModel != null) {
          // RedirectModel is already present: update model
          existRedirectModel.addAllAttributes(redirectModel);
        }
        else {
          // No RedirectModel is present: just set
          context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);
          bindingContext.setRedirectModel(redirectModel);
        }
      }
      else {
        bindingContext.addAllAttributes(model.asMap());
      }
    }
    else if (returnValue != null) {
      // should not happen
      HandlerMethod handlerMethod = getHandlerMethod(handler);
      if (handlerMethod != null) {
        throw new UnsupportedOperationException("Unexpected return type [" +
                handlerMethod.getReturnType().getParameterType().getName() + "] in method: " + handlerMethod.getMethod());
      }
    }
  }

  @Nullable
  @Override
  public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    if (resolvable.is(RedirectModel.class)) {
      RedirectModel redirectModel = new RedirectModel();
      context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);
      // set redirect model to current BindingContext
      context.getBindingContext().setRedirectModel(redirectModel);
      return redirectModel;
    }

    return context.getBindingContext().getModel();
  }

  @Nullable
  static HandlerMethod getHandlerMethod(Object handler) {
    if (handler instanceof HandlerMethod) {
      return (HandlerMethod) handler;
    }
    else if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      return annotationHandler.getMethod();
    }
    return null;
  }

}





