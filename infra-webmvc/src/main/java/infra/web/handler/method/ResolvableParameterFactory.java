/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.method;

import java.lang.reflect.Method;
import java.util.HashMap;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * Factory for creating {@link ResolvableMethodParameter} instances.
 * <p>This class is responsible for building arrays of resolvable method parameters,
 * optionally caching them based on the underlying {@link java.lang.reflect.Method}.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/3/21 13:58
 */
public class ResolvableParameterFactory {

  private static final ResolvableMethodParameter[] EMPTY = new ResolvableMethodParameter[0];

  private final ParameterNameDiscoverer parameterNameDiscoverer;

  private final HashMap<Method, ResolvableMethodParameter[]> cache = new HashMap<>();

  /**
   * Constructs a new {@code ResolvableParameterFactory}
   * using the shared instance of {@link ParameterNameDiscoverer}.
   */
  public ResolvableParameterFactory() {
    this(ParameterNameDiscoverer.getSharedInstance());
  }

  /**
   * Constructs a new {@code ResolvableParameterFactory} with the specified {@link ParameterNameDiscoverer}.
   *
   * @param parameterNameDiscoverer the discoverer used to resolve parameter names; must not be null
   * @throws IllegalArgumentException if {@code parameterNameDiscoverer} is null
   */
  public ResolvableParameterFactory(ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parameterNameDiscoverer, "parameterNameDiscoverer is required");
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Creates an array of {@link ResolvableMethodParameter} instances for the given handler method.
   * <p>This method initializes parameter name discovery for each parameter and converts them
   * into resolvable method parameters.</p>
   *
   * @param handlerMethod the handler method to process
   * @return an array of {@link ResolvableMethodParameter}, or an empty array if no parameters exist
   */
  public ResolvableMethodParameter[] createArray(HandlerMethod handlerMethod) {
    MethodParameter[] parameters = handlerMethod.getMethodParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return EMPTY;
    }
    int i = 0;
    ResolvableMethodParameter[] ret = new ResolvableMethodParameter[parameters.length];
    for (MethodParameter parameter : parameters) {
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
      ret[i++] = createParameter(parameter);
    }
    return ret;
  }

  /**
   * Retrieves the array of {@link ResolvableMethodParameter} instances for the given handler method,
   * using a cache to avoid redundant creation for the same underlying {@link java.lang.reflect.Method}.
   * <p>If the parameters are not found in the cache, they are created and stored for future access.</p>
   *
   * @param handlerMethod the handler method to retrieve parameters for
   * @return a cached or newly created array of {@link ResolvableMethodParameter}, or an empty array if no parameters exist
   */
  public ResolvableMethodParameter[] getParameters(HandlerMethod handlerMethod) {
    Method method = handlerMethod.getMethod();
    if (method.getParameterCount() == 0) {
      return EMPTY;
    }
    ResolvableMethodParameter[] parameters = cache.get(method);
    if (parameters == null) {
      parameters = createArray(handlerMethod);
      cache.put(method, parameters);
    }
    return parameters;
  }

  /**
   * Creates a new {@link ResolvableMethodParameter} instance from the given {@link MethodParameter}.
   *
   * @param parameter the method parameter to wrap
   * @return a new {@link ResolvableMethodParameter} instance
   */
  public ResolvableMethodParameter createParameter(MethodParameter parameter) {
    return new ResolvableMethodParameter(parameter);
  }

}
