/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.session;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * for {@link WebSession} Type-based parameter resolving
 * <p>
 * Like following example
 * <pre>
 * {@code
 *  // if request not contains a WebSession create new one
 *  @GET("/captcha")
 *  public BufferedImage captcha(WebSession session) {
 *     ...
 *     session.setAttribute(RAND_CODE, randCode);
 *     return image;
 *  }
 *  // WebSession may be null
 *  @GET("/test")
 *  public void nullable(@Nullable WebSession session) {
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
 * @see WebSession
 * @since 2019-09-27 22:36
 */
public class SessionMethodArgumentResolver
        extends SessionManagerOperations implements ParameterResolvingStrategy {

  public SessionMethodArgumentResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAssignableTo(WebSession.class);
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
