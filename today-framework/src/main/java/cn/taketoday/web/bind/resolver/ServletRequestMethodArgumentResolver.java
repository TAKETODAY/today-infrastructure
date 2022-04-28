/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.PushBuilder;

/**
 * Resolves servlet backed request-related method arguments. Supports values of the
 * following types:
 * <ul>
 * <li>{@link RequestContext}
 * <li>{@link ServletRequest}
 * <li>{@link HttpSession}
 * <li>{@link PushBuilder}
 * <li>{@link Principal} but only if not annotated in order to allow custom
 * resolvers to resolve it, and the falling back on
 * {@link PrincipalMethodArgumentResolver}.
 * <li>{@link InputStream}
 * <li>{@link Reader}
 * <li>{@link HttpMethod}
 * <li>{@link Locale}
 * <li>{@link TimeZone}
 * <li>{@link java.time.ZoneId}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 10:34
 */
public class ServletRequestMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    Class<?> paramType = resolvable.getParameterType();
    return ServletRequest.class.isAssignableFrom(paramType)
            || HttpSession.class.isAssignableFrom(paramType)
            || PushBuilder.class.isAssignableFrom(paramType)
            || (Principal.class.isAssignableFrom(paramType) && !resolvable.hasParameterAnnotations());
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    HttpServletRequest request = ((ServletRequestContext) context).getRequest();

    Class<?> paramType = resolvable.getParameterType();
    if (HttpSession.class.isAssignableFrom(paramType)) {
      HttpSession session = request.getSession();
      if (session != null && !paramType.isInstance(session)) {
        throw new IllegalStateException(
                "Current session is not of type [" + paramType.getName() + "]: " + session);
      }
      return session;
    }
    else if (PushBuilder.class.isAssignableFrom(paramType)) {
      PushBuilder pushBuilder = request.newPushBuilder();
      if (pushBuilder != null && !paramType.isInstance(pushBuilder)) {
        throw new IllegalStateException(
                "Current push builder is not of type [" + paramType.getName() + "]: " + pushBuilder);
      }
      return pushBuilder;
    }
    else if (Principal.class.isAssignableFrom(paramType)) {
      Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null && !paramType.isInstance(userPrincipal)) {
        throw new IllegalStateException(
                "Current user principal is not of type [" + paramType.getName() + "]: " + userPrincipal);
      }
      return userPrincipal;
    }

    // ServletRequest / HttpServletRequest
    return ServletUtils.getNativeRequest(request, paramType);
  }

}
