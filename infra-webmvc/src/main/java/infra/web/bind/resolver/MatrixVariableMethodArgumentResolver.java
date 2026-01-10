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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.CollectionUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.annotation.MatrixVariable;
import infra.web.bind.MissingMatrixVariableException;
import infra.web.bind.RequestBindingException;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves arguments annotated with {@link MatrixVariable @MatrixVariable}.
 *
 * <p>If the method parameter is of type {@link Map} it will by resolved by
 * {@link MatrixVariableMapMethodArgumentResolver} instead unless the annotation
 * specifies a name in which case it is considered to be a single attribute of
 * type map (vs multiple attributes collected in a map).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MatrixVariable
 * @since 4.0 2022/1/23 22:22
 */
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueResolvingStrategy {

  public MatrixVariableMethodArgumentResolver() {
    super(null);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (!resolvable.hasParameterAnnotation(MatrixVariable.class)) {
      return false;
    }
    if (Map.class.isAssignableFrom(resolvable.getParameterType())) {
      MatrixVariable matrixVariable = resolvable.getParameterAnnotation(MatrixVariable.class);
      return matrixVariable != null && StringUtils.hasText(matrixVariable.name());
    }
    return true;
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext request) throws Exception {
    Map<String, MultiValueMap<String, String>> pathParameters = request.matchingMetadata().getMatrixVariables();
    if (CollectionUtils.isEmpty(pathParameters)) {
      return null;
    }

    MethodParameter parameter = resolvable.getParameter();
    MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
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

