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

import java.util.Map;

import infra.core.ResolvableType;
import infra.web.BindingContext;
import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * Resolves {@code Map<String, Object> model} method arguments and handles {@link Map} return values.
 *
 * <p>A Map return value can be interpreted in more than one ways depending
 * on the presence of annotations like {@code @ModelAttribute} or
 * {@code @ResponseBody}. this resolver returns false if the
 * parameter is annotated.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerMethod
 */
public class MapMethodProcessor implements ParameterResolvingStrategy, HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (resolvable.is(Map.class) && resolvable.getParameterAnnotations().length == 0) {
      // Map<String, Object> model;
      ResolvableType mapType = resolvable.getResolvableType().asMap();
      ResolvableType keyType = mapType.getGeneric(0);
      ResolvableType valueType = mapType.getGeneric(1);
      return keyType.resolve() == String.class
              && valueType.resolve() == Object.class;
    }

    return false;
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    return context.binding().getModel();
  }

  // HandlerMethodReturnValueHandler

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(Map.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof Map;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void handleReturnValue(RequestContext context,
          @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Map map) {
      BindingContext bindingContext = context.getBinding();
      if (bindingContext != null) {
        bindingContext.addAllAttributes(map);
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
