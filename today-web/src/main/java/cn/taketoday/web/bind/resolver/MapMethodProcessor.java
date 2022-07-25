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

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;

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
public class MapMethodProcessor implements ParameterResolvingStrategy, SmartReturnValueHandler {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (resolvable.getParameterType() == Map.class
            && resolvable.getParameterAnnotations().length == 0) {
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
    return context.getBindingContext().getModel();
  }

  // HandlerMethodReturnValueHandler

  @Override
  public boolean supportsHandler(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return handlerMethod.isReturnTypeAssignableTo(Map.class);
    }
    return false;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof Map;
  }

  @Override
  @SuppressWarnings({ "unchecked" })
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Map map) {
      context.getBindingContext().addAllAttributes(map);
    }
    else if (returnValue != null) {
      // should not happen
      HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
      if (handlerMethod != null) {
        throw new UnsupportedOperationException("Unexpected return type [" +
                handlerMethod.getReturnType().getParameterType().getName() + "] in method: " + handlerMethod.getMethod());
      }
    }
  }

}
