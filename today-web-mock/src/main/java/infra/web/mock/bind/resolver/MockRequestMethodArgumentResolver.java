/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.mock.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.security.Principal;

import infra.mock.api.MockRequest;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpSession;
import infra.mock.api.http.PushBuilder;
import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;

/**
 * Resolves servlet backed request-related method arguments. Supports values of the
 * following types:
 * <ul>
 * <li>{@link MockRequest}
 * <li>{@link HttpSession}
 * <li>{@link PushBuilder}
 * <li>{@link Principal} but only if not annotated in order to allow custom
 * resolvers to resolve it, and the falling back on
 * {@link PrincipalMethodArgumentResolver}.
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
            || HttpSession.class.isAssignableFrom(paramType)
            || PushBuilder.class.isAssignableFrom(paramType)
            || (Principal.class.isAssignableFrom(paramType) && !resolvable.hasParameterAnnotations());
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    HttpMockRequest request = ((MockRequestContext) context).getRequest();

    Class<?> paramType = resolvable.getParameterType();
    if (HttpSession.class.isAssignableFrom(paramType)) {
      HttpSession session = request.getSession();
      if (session != null && !paramType.isInstance(session)) {
        throw new IllegalStateException(
                "Current session is not of type [%s]: %s".formatted(paramType.getName(), session));
      }
      return session;
    }
    else if (PushBuilder.class.isAssignableFrom(paramType)) {
      PushBuilder pushBuilder = request.newPushBuilder();
      if (pushBuilder != null && !paramType.isInstance(pushBuilder)) {
        throw new IllegalStateException(
                "Current push builder is not of type [%s]: %s".formatted(paramType.getName(), pushBuilder));
      }
      return pushBuilder;
    }
    else if (Principal.class.isAssignableFrom(paramType)) {
      Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null && !paramType.isInstance(userPrincipal)) {
        throw new IllegalStateException(
                "Current user principal is not of type [%s]: %s".formatted(paramType.getName(), userPrincipal));
      }
      return userPrincipal;
    }

    // ServletRequest / HttpServletRequest
    return MockUtils.getNativeRequest(request, paramType);
  }

}
