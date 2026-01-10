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

import java.util.List;
import java.util.Map;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.annotation.MatrixVariable;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves arguments of type {@link Map} annotated with {@link MatrixVariable @MatrixVariable}
 * where the annotation does not specify a name. In other words the purpose of this resolver
 * is to provide access to multiple matrix variables, either all or associated with a specific
 * path variable.
 *
 * <p>When a name is specified, an argument of type Map is considered to be a single attribute
 * with a Map value, and is resolved by {@link MatrixVariableMethodArgumentResolver} instead.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/23 22:23
 */
public class MatrixVariableMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    MatrixVariable variable = resolvable.getParameterAnnotation(MatrixVariable.class);
    return variable != null
            && Map.class.isAssignableFrom(resolvable.getParameterType())
            && StringUtils.isBlank(variable.name());
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    var matrixVariables = context.matchingMetadata().getMatrixVariables();
    MethodParameter parameter = resolvable.getParameter();
    MultiValueMap<String, String> map = mapMatrixVariables(parameter, matrixVariables);
    return isSingleValueMap(parameter) ? map.toSingleValueMap() : map;
  }

  private MultiValueMap<String, String> mapMatrixVariables(MethodParameter parameter,
          @Nullable Map<String, MultiValueMap<String, String>> matrixVariables) {

    MultiValueMap<String, String> map = MultiValueMap.forLinkedHashMap();
    if (CollectionUtils.isEmpty(matrixVariables)) {
      return map;
    }
    MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
    Assert.state(ann != null, "No MatrixVariable annotation");
    String pathVariable = ann.pathVar();

    if (!pathVariable.equals(Constant.DEFAULT_NONE)) {
      MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
      if (mapForPathVariable == null) {
        return map;
      }
      map.putAll(mapForPathVariable);
    }
    else {
      for (MultiValueMap<String, String> vars : matrixVariables.values()) {
        for (Map.Entry<String, List<String>> entry : vars.entrySet()) {
          String name = entry.getKey();
          List<String> values = entry.getValue();
          for (String value : values) {
            map.add(name, value);
          }
        }
      }
    }
    return map;
  }

  private boolean isSingleValueMap(MethodParameter parameter) {
    if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
      ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
      if (genericTypes.length == 2) {
        return !List.class.isAssignableFrom(genericTypes[1].toClass());
      }
    }
    return false;
  }

}

