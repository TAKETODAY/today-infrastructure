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

import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.MatrixVariable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

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

