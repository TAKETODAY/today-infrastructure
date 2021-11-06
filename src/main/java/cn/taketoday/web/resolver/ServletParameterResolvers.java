/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ServletContextAttribute;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.servlet.ServletUtils;

/**
 * @author TODAY <br>
 * 2019-07-12 18:04
 */
public class ServletParameterResolvers {

  public static void register(List<ParameterResolvingStrategy> resolvers, ServletContext context) {
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
  }

  static class ServletRequestParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletRequest.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return ServletUtils.getServletRequest(context);
    }
  }

  static class ServletResponseParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isInterface() && parameter.isAssignableTo(ServletResponse.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return ServletUtils.getServletResponse(context);
    }
  }

  static class HttpSessionParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isAssignableTo(HttpSession.class);
    }

    @Override
    public Object resolveParameter(
            RequestContext context, MethodParameter parameter) throws Throwable {
      return ServletUtils.getHttpSession(context);
    }
  }

  static class HttpSessionAttributeParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isAnnotationPresent(SessionAttribute.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      HttpSession httpSession = ServletUtils.getHttpSession(context, false);
      if (httpSession == null) {
        return null;
      }
      return httpSession.getAttribute(parameter.getName());
    }
  }

  static class ServletContextParameterResolver implements ParameterResolvingStrategy {

    private final ServletContext servletContext;

    public ServletContextParameterResolver(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.is(ServletContext.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return servletContext;
    }
  }

  // ------------- cookie

  static class ServletCookieParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.is(Cookie.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {

      String name = parameter.getName();
      for (Cookie cookie : context.unwrapRequest(HttpServletRequest.class).getCookies()) {
        if (name.equals(cookie.getName())) {
          return cookie;
        }
      }
      // no cookie
      if (parameter.isRequired()) {
        throw new MissingParameterException("Cookie", parameter);
      }
      return null;
    }
  }

  static class ServletCookieCollectionParameterResolver
          extends CollectionParameterResolver implements ParameterResolvingStrategy {

    @Override
    protected boolean supportsInternal(MethodParameter parameter) {
      return parameter.isGenericPresent(Cookie.class, 0);
    }

    @Override
    protected List<?> resolveCollection(RequestContext context, MethodParameter parameter) {
      Cookie[] cookies = context.unwrapRequest(HttpServletRequest.class).getCookies();
      ArrayList<Cookie> ret = new ArrayList<>(cookies.length);
      Collections.addAll(ret, cookies);
      return ret;
    }
  }

  static class ServletCookieArrayParameterResolver implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isArray() && parameter.getParameterClass().getComponentType() == Cookie.class;
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return context.unwrapRequest(HttpServletRequest.class).getCookies();
    }
  }

  static class ServletContextAttributeParameterResolver implements ParameterResolvingStrategy {
    private final ServletContext servletContext;

    public ServletContextAttributeParameterResolver(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.isAnnotationPresent(ServletContextAttribute.class);
    }

    @Override
    public Object resolveParameter(RequestContext context, MethodParameter parameter) throws Throwable {
      return servletContext.getAttribute(parameter.getName());
    }
  }
}
