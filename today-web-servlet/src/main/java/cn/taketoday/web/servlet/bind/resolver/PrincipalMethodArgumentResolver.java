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

package cn.taketoday.web.servlet.bind.resolver;

import java.security.Principal;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.mock.http.HttpServletRequest;

/**
 * Resolves an argument of type {@link Principal}, similar to
 * {@link ServletRequestMethodArgumentResolver} but irrespective of whether the
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
    HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);

    Principal principal = servletRequest.getUserPrincipal();
    if (principal != null && !resolvable.getParameterType().isInstance(principal)) {
      throw new IllegalStateException("Current user principal is not of type [%s]: %s"
              .formatted(resolvable.getParameterType().getName(), principal));
    }

    return principal;
  }

}
