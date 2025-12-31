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

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * for {@link Session} Type-based parameter resolving
 * <p>
 * Like following example
 * <pre>
 * {@code
 *  // if request not contains a Session create new one
 *  @GET("/captcha")
 *  public BufferedImage captcha(Session session) {
 *     ...
 *     session.setAttribute(RAND_CODE, randCode);
 *     return image;
 *  }
 *  // Session may be null
 *  @GET("/test")
 *  public void nullable(@Nullable Session session) {
 *     ...
 *     if (session == null) {
 *
 *     }
 *     else {
 *
 *     }
 *  }
 *
 * }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Session
 * @since 2019-09-27 22:36
 */
public class SessionMethodArgumentResolver extends SessionManagerOperations implements ParameterResolvingStrategy {

  public SessionMethodArgumentResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAssignableTo(Session.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) {
    // todo type checking?
    if (resolvable.isRequired()) {
      return getSession(context);
    }
    // Nullable
    return getSession(context, false);
  }

}
