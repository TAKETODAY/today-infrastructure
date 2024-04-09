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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.MatrixParam;
import cn.taketoday.web.bind.MissingMatrixVariableException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves arguments annotated with {@link MatrixParam @MatrixParam}.
 *
 * <p>If the method parameter is of type {@link Map} it will by resolved by
 * {@link MatrixParamMapMethodArgumentResolver} instead unless the annotation
 * specifies a name in which case it is considered to be a single attribute of
 * type map (vs multiple attributes collected in a map).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MatrixParam
 * @since 4.0 2022/1/23 22:22
 */
public class MatrixParamMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

  public MatrixParamMethodArgumentResolver() {
    super(null);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (!resolvable.hasParameterAnnotation(MatrixParam.class)) {
      return false;
    }
    if (Map.class.isAssignableFrom(resolvable.getParameterType())) {
      MatrixParam matrixVariable = resolvable.getParameterAnnotation(MatrixParam.class);
      return matrixVariable != null && StringUtils.hasText(matrixVariable.name());
    }
    return true;
  }

  @Nullable
  @Override
  protected Object resolveName(
          String name, ResolvableMethodParameter resolvable, RequestContext request) throws Exception {
    Map<String, MultiValueMap<String, String>> pathParameters = request.matchingMetadata().getMatrixVariables();
    if (CollectionUtils.isEmpty(pathParameters)) {
      return null;
    }

    MethodParameter parameter = resolvable.getParameter();
    MatrixParam ann = parameter.getParameterAnnotation(MatrixParam.class);
    Assert.state(ann != null, "No MatrixVariable annotation");
    String pathVar = ann.pathVar();
    List<String> paramValues = null;

    if (!pathVar.equals(Constant.DEFAULT_NONE)) {
      if (pathParameters.containsKey(pathVar)) {
        paramValues = pathParameters.get(pathVar).get(name);
      }
    }
    else {
      boolean found = false;
      paramValues = new ArrayList<>();
      for (MultiValueMap<String, String> params : pathParameters.values()) {
        if (params.containsKey(name)) {
          if (found) {
            String paramType = parameter.getNestedParameterType().getName();
            throw new RequestBindingException(
                    "Found more than one match for URI path parameter '%s' for parameter type [%s]. Use 'pathVar' attribute to disambiguate."
                            .formatted(name, paramType));
          }
          paramValues.addAll(params.get(name));
          found = true;
        }
      }
    }

    if (CollectionUtils.isEmpty(paramValues)) {
      return null;
    }
    else if (paramValues.size() == 1) {
      return paramValues.get(0);
    }
    else {
      return paramValues;
    }
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter) throws RequestBindingException {
    throw new MissingMatrixVariableException(name, parameter);
  }

  @Override
  protected void handleMissingValueAfterConversion(
          String name, MethodParameter parameter, RequestContext request) throws Exception {
    throw new MissingMatrixVariableException(name, parameter, true);
  }

}

