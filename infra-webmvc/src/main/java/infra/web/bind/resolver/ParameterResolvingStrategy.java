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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.web.HttpContext;
import infra.web.bind.MethodParameterResolvingException;
import infra.web.handler.method.HandlerParameter;

/**
 * Strategy for resolving a handler method parameter to an argument value for
 * the current request.
 *
 * <p>Implementations first declare applicability in
 * {@link #supportsParameter(HandlerParameter)} and then perform the actual
 * resolution in {@link #resolveArgument(HttpContext, HandlerParameter)}.
 * A {@link ParameterResolvingRegistry} consults registered strategies in order
 * and delegates to the first one that supports the parameter.
 *
 * <p>Strategies may read from request parameters, path variables, headers,
 * cookies, the request body, or the model. They are also responsible for
 * converting the resolved value to the declared parameter type and may perform
 * data binding and validation before returning it.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * class SessionAttributeResolver implements ParameterResolvingStrategy {
 *
 *   @Override
 *   public boolean supportsParameter(HandlerParameter parameter) {
 *     return parameter.hasParameterAnnotation(SessionAttribute.class);
 *   }
 *
 *   @Override
 *   public @Nullable Object resolveArgument(HttpContext context, HandlerParameter parameter) {
 *     Session session = context.getSession(false);
 *     return session != null ? session.getAttribute(parameter.getName()) : null;
 *   }
 * }
 * }</pre>
 *
 * <p>See {@link infra.web.handler.method.ModelAttributeMethodProcessor} for a
 * built-in strategy that binds request values to model attributes and optionally
 * validates them.
 *
 * @author TODAY 2019-07-07 23:24
 * @see HandlerParameter
 * @see ParameterResolvingRegistry
 * @see MethodParameterResolvingException
 * @since 3.0
 */
public interface ParameterResolvingStrategy {

  /**
   * Determine whether this strategy can resolve the given parameter.
   *
   * <p>This method is called while selecting a strategy for a parameter and
   * should have no side effects. Implementations typically inspect the
   * parameter's annotations, type, or other metadata. Returning {@code true}
   * means that {@link #resolveArgument(HttpContext, HandlerParameter)} can
   * handle the parameter; it does not guarantee that resolution will succeed.
   *
   * @param parameter the parameter to inspect
   * @return {@code true} if this strategy supports the parameter,
   * {@code false} otherwise
   */
  boolean supportsParameter(HandlerParameter parameter);

  /**
   * Resolve the argument value for the given parameter.
   *
   * <p>This method is invoked only after
   * {@link #supportsParameter(HandlerParameter)} has returned {@code true} for
   * the parameter. Implementations obtain the value from the current request or
   * model context, convert it to the declared parameter type if necessary, and
   * return the value to be passed to the handler method.
   *
   * @param context the current request context
   * @param parameter the parameter to resolve
   * @return the resolved argument value, or {@code null} if the value is
   * absent or resolves to {@code null}
   * @throws Exception if the argument cannot be resolved
   */
  @Nullable
  Object resolveArgument(HttpContext context, HandlerParameter parameter) throws Exception;

}
