/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.ui.Model;
import infra.web.BindingContext;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;

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
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
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





