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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.Model;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

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
public class ModelMethodProcessor implements ReturnValueHandler, ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.isAssignableTo(Model.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    if (resolvable.is(RedirectModel.class)) {
      RedirectModel redirectModel = new RedirectModel();
      context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);
      // set redirect model to current BindingContext
      context.binding().setRedirectModel(redirectModel);
      return redirectModel;
    }

    return context.binding().getModel();
  }

  //---------------------------------------------------------------------
  // Implementation of ReturnValueHandler interface
  //---------------------------------------------------------------------

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof Model;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return handlerMethod.isReturnTypeAssignableTo(Model.class);
    }
    return false;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Model model) {
      BindingContext bindingContext = context.binding();
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
      HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
      if (handlerMethod != null) {
        throw new UnsupportedOperationException("Unexpected return type [%s] in method: %s"
                .formatted(handlerMethod.getReturnType().getParameterType().getName(), handlerMethod.getMethod()));
      }
    }
  }

}





