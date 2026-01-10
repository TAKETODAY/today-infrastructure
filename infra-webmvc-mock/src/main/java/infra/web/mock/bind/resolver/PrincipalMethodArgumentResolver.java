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

package infra.web.mock.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.security.Principal;

import infra.mock.api.http.HttpMockRequest;
import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockUtils;

/**
 * Resolves an argument of type {@link Principal}, similar to
 * {@link MockRequestMethodArgumentResolver} but irrespective of whether the
 * argument is annotated or not. This is done to enable custom argument
 * resolution of a {@link Principal} argument (with a custom annotation).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 10:36
 */
public class PrincipalMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return Principal.class.isAssignableFrom(resolvable.getParameterType());
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    HttpMockRequest servletRequest = MockUtils.getMockRequest(context);

    Principal principal = servletRequest.getUserPrincipal();
    if (principal != null && !resolvable.getParameterType().isInstance(principal)) {
      throw new IllegalStateException("Current user principal is not of type [%s]: %s"
              .formatted(resolvable.getParameterType().getName(), principal));
    }

    return principal;
  }

}
