/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.ResolvableType;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestParam;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;

/**
 * Resolves {@link Map} method arguments annotated with an @{@link RequestParam}
 * where the annotation does not specify a request parameter name.
 *
 * <p>The created {@link Map} contains all request parameter name/value pairs,
 * or all multipart files for a given parameter name if specifically declared
 * with {@link Part} as the value type. If the method parameter type is
 * {@link MultiValueMap} instead, the created map contains all request parameters
 * and all their values for cases where request parameters have multiple values
 * (or multiple multipart files of the same name).
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestParamMethodArgumentResolver
 * @see RequestContext#getParameters()
 * @see MultipartRequest#getParts()
 * @since 4.0 2022/4/28 15:26
 */
public class RequestParamMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (Map.class.isAssignableFrom(resolvable.getParameterType())) {
      RequestParam requestParam = resolvable.getParameterAnnotation(RequestParam.class);
      return requestParam != null
              && StringUtils.isBlank(requestParam.name());
    }
    return false;
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    ResolvableType resolvableType = resolvable.getResolvableType();

    if (MultiValueMap.class.isAssignableFrom(resolvable.getParameterType())) {
      // MultiValueMap
      Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
      if (valueType == Part.class) {
        return context.asMultipartRequest().getParts();
      }
      else {
        return context.getParameters();
      }
    }

    else {
      // Regular Map
      Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
      if (valueType == Part.class) {
        return context.asMultipartRequest().getParts().toSingleValueMap();
      }
      else {
        return context.getParameters().toSingleValueMap();
      }
    }
  }

}
