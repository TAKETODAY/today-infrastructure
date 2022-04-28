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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ServletContextAttribute;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * @author TODAY <br>
 * 2019-07-12 18:04
 */
public class ServletParameterResolvers {

  public static void register(ParameterResolvingStrategies resolvers, ServletContext context) {
    resolvers.add(new ServletRequestMethodArgumentResolver());
    // Servlet cookies parameter
    // ----------------------------
    resolvers.add(new ServletCookieParameterResolver());
    resolvers.add(new ServletCookieArrayParameterResolver());
    resolvers.add(new ServletCookieCollectionParameterResolver());
    // Servlet components parameter
    // ----------------------------
    resolvers.add(new HttpSessionParameterResolver());
    resolvers.add(new ServletRequestParameterResolver());
    resolvers.add(new ServletResponseParameterResolver());
    resolvers.add(new ServletContextParameterResolver(context));
    // Attributes
    // ------------------------
    resolvers.add(new HttpSessionAttributeParameterResolver());
    resolvers.add(new ServletContextAttributeParameterResolver(context));
    resolvers.add(new PrincipalMethodArgumentResolver());
  }

  static class ServletRequestParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletRequest.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletRequest(context);
    }
  }

  static class ServletResponseParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletResponse.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletResponse(context);
    }
  }

  static class HttpSessionParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isAssignableTo(HttpSession.class);
    }

    @Override
    public Object resolveArgument(
            RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getHttpSession(context);
    }
  }

  static class HttpSessionAttributeParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(SessionAttribute.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      HttpSession httpSession = ServletUtils.getHttpSession(context, false);
      if (httpSession == null) {
        return null;
      }
      return httpSession.getAttribute(resolvable.getName());
    }
  }

  static class ServletContextParameterResolver implements ParameterResolvingStrategy {

    private final ServletContext servletContext;

    public ServletContextParameterResolver(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.is(ServletContext.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return servletContext;
    }
  }

  // ------------- cookie

  static class ServletCookieParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.is(Cookie.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {

      String name = resolvable.getName();
      for (Cookie cookie : context.unwrapRequest(HttpServletRequest.class).getCookies()) {
        if (name.equals(cookie.getName())) {
          return cookie;
        }
      }
      // no cookie
      if (resolvable.isRequired()) {
        throw new MissingRequestCookieException(name, resolvable.getParameter());
      }
      return null;
    }
  }

  static class ServletCookieCollectionParameterResolver
          extends CollectionParameterResolver implements ParameterResolvingStrategy {

    @Override
    protected boolean supportsInternal(ResolvableMethodParameter resolvable) {
      return resolvable.getResolvableType().asCollection().getGeneric(0).resolve() == Cookie.class;
    }

    @Override
    protected List<?> resolveCollection(RequestContext context, ResolvableMethodParameter parameter) {
      Cookie[] cookies = ServletUtils.getServletRequest(context).getCookies();
      ArrayList<Cookie> ret = new ArrayList<>(cookies.length);
      Collections.addAll(ret, cookies);
      return ret;
    }
  }

  static class ServletCookieArrayParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.isArray() && parameter.getParameterType().getComponentType() == Cookie.class;
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return ServletUtils.getServletRequest(context).getCookies();
    }
  }

  static class ServletContextAttributeParameterResolver implements ParameterResolvingStrategy {
    private final ServletContext servletContext;

    public ServletContextAttributeParameterResolver(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter parameter) {
      return parameter.hasParameterAnnotation(ServletContextAttribute.class);
    }

    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return servletContext.getAttribute(resolvable.getName());
    }
  }
}
