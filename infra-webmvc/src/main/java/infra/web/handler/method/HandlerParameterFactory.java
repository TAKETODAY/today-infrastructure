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
import infra.util.Assert;
import infra.util.ObjectUtils;

/**
 * Factory for creating {@link HandlerParameter} instances.
 * <p>This class is responsible for building arrays of resolvable method parameters,
 * optionally caching them based on the underlying {@link java.lang.reflect.Method}.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/3/21 13:58
 */
public class HandlerParameterFactory {

  private static final HandlerParameter[] EMPTY = new HandlerParameter[0];

  private final ParameterNameDiscoverer parameterNameDiscoverer;

  private final HashMap<Method, HandlerParameter[]> cache = new HashMap<>();

  /**
   * Constructs a new {@code ResolvableParameterFactory}
   * using the shared instance of {@link ParameterNameDiscoverer}.
   */
  public HandlerParameterFactory() {
    this(ParameterNameDiscoverer.getSharedInstance());
  }

  /**
   * Constructs a new {@code ResolvableParameterFactory} with the specified {@link ParameterNameDiscoverer}.
   *
   * @param parameterNameDiscoverer the discoverer used to resolve parameter names; must not be null
   * @throws IllegalArgumentException if {@code parameterNameDiscoverer} is null
   */
  public HandlerParameterFactory(ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parameterNameDiscoverer, "parameterNameDiscoverer is required");
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Creates an array of {@link HandlerParameter} instances for the given handler method.
   * <p>This method initializes parameter name discovery for each parameter and converts them
   * into resolvable method parameters.</p>
   *
   * @param handlerMethod the handler method to process
   * @return an array of {@link HandlerParameter}, or an empty array if no parameters exist
   */
  public HandlerParameter[] createArray(HandlerMethod handlerMethod) {
    MethodParameter[] parameters = handlerMethod.getParameters();
    if (ObjectUtils.isEmpty(parameters)) {
      return EMPTY;
    }
    int i = 0;
    HandlerParameter[] ret = new HandlerParameter[parameters.length];
    for (MethodParameter parameter : parameters) {
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
      ret[i++] = createParameter(parameter);
    }
    return ret;
  }

  /**
   * Retrieves the array of {@link HandlerParameter} instances for the given handler method,
   * using a cache to avoid redundant creation for the same underlying {@link java.lang.reflect.Method}.
   * <p>If the parameters are not found in the cache, they are created and stored for future access.</p>
   *
   * @param handlerMethod the handler method to retrieve parameters for
   * @return a cached or newly created array of {@link HandlerParameter}, or an empty array if no parameters exist
   */
  public HandlerParameter[] getParameters(HandlerMethod handlerMethod) {
    Method method = handlerMethod.getMethod();
    if (method.getParameterCount() == 0) {
      return EMPTY;
    }
    HandlerParameter[] parameters = cache.get(method);
    if (parameters == null) {
      parameters = createArray(handlerMethod);
      cache.put(method, parameters);
    }
    return parameters;
  }

  /**
   * Creates a new {@link HandlerParameter} instance from the given {@link MethodParameter}.
   *
   * @param parameter the method parameter to wrap
   * @return a new {@link HandlerParameter} instance
   */
  public HandlerParameter createParameter(MethodParameter parameter) {
    return new HandlerParameter(parameter);
  }

}
