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

import infra.web.RequestContext;
import infra.web.bind.MethodParameterResolvingException;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Strategy interface for resolving method parameters in the context of a web request.
 * <p>
 * Implementations of this interface are responsible for determining whether they can
 * handle a specific method parameter and resolving the value of that parameter based on
 * the current request context.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * Below is an example implementation of {@code ParameterResolvingStrategy} that resolves
 * a method parameter annotated with {@code @SessionAttribute}:
 * <pre>{@code
 * static class ForHttpSessionAttribute implements ParameterResolvingStrategy {
 *
 *   @Override
 *   public boolean supportsParameter(ResolvableMethodParameter parameter) {
 *     return parameter.hasParameterAnnotation(SessionAttribute.class);
 *   }
 *
 *   @Override
 *   public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
 *     HttpSession httpSession = getHttpSession(context, false);
 *     if (httpSession == null) {
 *       return null;
 *     }
 *     return httpSession.getAttribute(resolvable.getName());
 *   }
 * }
 * }</pre>
 *
 * Another example is the {@code ModelAttributeMethodProcessor}, which handles parameters
 * annotated with {@code @ModelAttribute} or non-simple types in default resolution mode.
 * It resolves the parameter by binding request values to the model attribute and optionally
 * validating it.
 *
 * @author TODAY 2019-07-07 23:24
 * @see ResolvableMethodParameter
 * @see MethodParameterResolvingException
 */
public interface ParameterResolvingStrategy {

  /**
   * Determines whether this resolver supports the given method parameter.
   * <p>
   * This method is typically used to check if the resolver can handle a specific
   * parameter based on its annotations, type, or other attributes. For example,
   * a resolver might support parameters annotated with a specific annotation or
   * parameters of a certain type.
   * </p>
   *
   * <h3>Usage Example:</h3>
   * Below is an example of how this method might be implemented to support
   * parameters annotated with {@code @SessionAttribute}:
   * <pre>{@code
   * static class ForHttpSessionAttribute implements ParameterResolvingStrategy {
   *
   *   @Override
   *   public boolean supportsParameter(ResolvableMethodParameter parameter) {
   *     return parameter.hasParameterAnnotation(SessionAttribute.class);
   *   }
   * }
   * }</pre>
   *
   * Another example could involve supporting non-annotated parameters of a specific type:
   * <pre>{@code
   * static class ForCustomType implements ParameterResolvingStrategy {
   *
   *   @Override
   *   public boolean supportsParameter(ResolvableMethodParameter parameter) {
   *     return CustomType.class.isAssignableFrom(parameter.getParameterType());
   *   }
   * }
   * }</pre>
   *
   * @param resolvable the method parameter to check
   * @return {@code true} if this resolver supports the parameter; {@code false} otherwise
   */
  boolean supportsParameter(ResolvableMethodParameter resolvable);

  /**
   * Resolves the argument for the given method parameter within the context of a request.
   * <p>
   * This method is invoked when the {@link #supportsParameter(ResolvableMethodParameter)}
   * method has determined that this resolver supports the specified parameter. It is
   * responsible for providing the actual value to be injected into the method parameter.
   * </p>
   *
   * @param context the request context containing information about the current request
   * @param resolvable the method parameter to resolve
   * @return the resolved argument value, or {@code null} if the value cannot be resolved
   * @throws Throwable if an error occurs during argument resolution
   */
  @Nullable
  Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable;

}
