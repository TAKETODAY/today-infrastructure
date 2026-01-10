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
