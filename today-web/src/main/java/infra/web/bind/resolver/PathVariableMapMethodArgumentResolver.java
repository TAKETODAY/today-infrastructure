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

import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.annotation.PathVariable;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves {@link Map} method arguments annotated with an @{@link PathVariable}
 * where the annotation does not specify a path variable name. The created
 * {@link Map} contains all URI template name/value pairs.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 17:52
 */
public class PathVariableMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    PathVariable ann = resolvable.getParameterAnnotation(PathVariable.class);
    return ann != null
            && Map.class.isAssignableFrom(resolvable.getParameterType())
            && StringUtils.isBlank(ann.value());
  }

  /**
   * Return a Map with all URI template variables or an empty map.
   */
  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    HandlerMatchingMetadata metadata = context.getMatchingMetadata();
    Map<Object, Object> map = CollectionUtils.createMap(resolvable.getParameterType());
    if (metadata != null) {
      Map<String, String> uriVariables = metadata.getUriVariables();
      if (CollectionUtils.isNotEmpty(uriVariables)) {
        map.putAll(uriVariables);
      }
    }
    return map;
  }

}
