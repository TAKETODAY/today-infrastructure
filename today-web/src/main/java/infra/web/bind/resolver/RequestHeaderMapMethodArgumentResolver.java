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

import infra.http.HttpHeaders;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.annotation.RequestHeader;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves {@link Map} method arguments annotated with {@code @RequestHeader}.
 * For individual header values annotated with {@code @RequestHeader} see
 * {@link RequestHeaderMethodArgumentResolver} instead.
 *
 * <p>The created {@link Map} contains all request header name/value pairs.
 * The method parameter type may be a {@link MultiValueMap} to receive all
 * values for a header, not only the first one.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/27 17:45
 */
public class RequestHeaderMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(HttpHeaders.class) ||
            (resolvable.hasParameterAnnotation(RequestHeader.class) && resolvable.isAssignableTo(Map.class));
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    HttpHeaders headers = context.getHeaders();
    Class<?> paramType = resolvable.getParameterType();
    if (paramType == HttpHeaders.class) {
      return headers;
    }

    // @RequestHeader

    if (MultiValueMap.class.isAssignableFrom(paramType)) {
      if (MultiValueMap.class == paramType) {
        return headers;
      }
      else {
        // target map
        Map<Object, Object> map = CollectionUtils.createMap(paramType, null, headers.size());
        map.putAll(headers);
        return map;
      }
    }
    else {
      Map<String, String> singleValueMap = headers.toSingleValueMap();
      if (Map.class == paramType) {
        return singleValueMap; //
      }
      // target map
      Map<Object, Object> map = CollectionUtils.createMap(paramType, null, headers.size());
      map.putAll(singleValueMap);
      return map;
    }
  }

}
