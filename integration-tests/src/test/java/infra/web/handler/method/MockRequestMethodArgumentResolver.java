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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import infra.web.mock.MockRequest;
import infra.session.Session;
import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.mock.MockRequestContext;

/**
 * Resolves servlet backed request-related method arguments. Supports values of the
 * following types:
 * <ul>
 * <li>{@link MockRequest}
 * <li>{@link Session}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 10:34
 */
public class MockRequestMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    Class<?> paramType = resolvable.getParameterType();
    return MockRequest.class.isAssignableFrom(paramType)
            || Session.class.isAssignableFrom(paramType);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    MockRequest request = ((MockRequestContext) context).getRequest();

    Class<?> paramType = resolvable.getParameterType();
    if (Session.class.isAssignableFrom(paramType)) {
      Session session = request.getSession();
      if (session != null && !paramType.isInstance(session)) {
        throw new IllegalStateException(
                "Current session is not of type [%s]: %s".formatted(paramType.getName(), session));
      }
      return session;
    }

    return request;
  }

}
