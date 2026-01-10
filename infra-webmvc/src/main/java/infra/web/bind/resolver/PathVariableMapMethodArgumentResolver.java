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
